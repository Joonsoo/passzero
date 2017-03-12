package com.giyeok.passzero.ui.swt

import java.util.concurrent.atomic.AtomicLong
import com.giyeok.passzero.utils.FutureStream
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.MouseEvent
import org.eclipse.swt.events.MouseListener
import org.eclipse.swt.events.MouseWheelListener
import org.eclipse.swt.events.PaintEvent
import org.eclipse.swt.events.PaintListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display

trait SortedListItem {
    def >(other: SortedListItem): Boolean // = a > b
    def dimension(gc: GC): Point
    def draw(gc: GC, bounds: Rectangle, selected: Boolean): Unit
}

case class TextSortedListItem[T](data: T, text: String) extends SortedListItem {
    def >(other: SortedListItem): Boolean = other match {
        case otherItem: TextSortedListItem[_] => text < otherItem.text
    }
    def dimension(gc: GC): Point = {
        val p = gc.textExtent(text)
        p.x *= -1
        p.y += 4
        p
    }
    def draw(gc: GC, bounds: Rectangle, selected: Boolean): Unit = {
        if (selected) {
            gc.setBackground(SortedList.selectedBackgroundColor)
            gc.fillRectangle(bounds)
        } else {
            gc.setBackground(SortedList.baseBackgroundColor)
        }
        gc.drawText(text, bounds.x + 4, bounds.y + 2)
    }
}

object SortedList {
    val selectedBackgroundColor = new Color(null, 255, 255, 0)
    val baseBackgroundColor = new Color(null, 255, 255, 255)
}

// TODO 나중에 StructuredTextView로 바꾸는 것을 고려
class SortedList[T <: SortedListItem](display: Display, parent: Composite, style: Int) extends Canvas(parent, style) {
    private var items = Seq[T]()
    private var allDimension: Option[Point] = None
    private var itemBoundsMap = Map[(T, Int), Rectangle]()

    private var _selectedItem = Option.empty[Int]
    def selectedItem: Option[(T, Int)] = _selectedItem map { idx => (items(idx), idx) }

    private val sourceIdCounter = new AtomicLong(0)
    private def newSourceId(): Long = sourceIdCounter.incrementAndGet()

    private var _progress: Boolean = false
    private def setProgress(progress: Boolean): Unit = {
        _progress = progress
        redraw()
    }

    private var _scroll: Point = new Point(0, 0)

    private var _listeners: Seq[Option[(T, Int)] => Unit] = Seq()

    setBackground(SortedList.baseBackgroundColor)

    def clear(): Unit = {
        items = Seq()
        allDimension = None
    }

    def setSource(stream: FutureStream[Seq[T]]): Unit = {
        clear()
        setProgress(true)
        val currentSourceId = newSourceId()
        stream foreach { (page, tail) =>
            this.synchronized {
                if (this.sourceIdCounter.get() == currentSourceId) {
                    page foreach { item =>
                        val index = items.zipWithIndex find { p => p._1 > item } map { _._2 } getOrElse items.length
                        val (init, tail) = items.splitAt(index)
                        val newList: Seq[T] = init ++ (item +: tail)
                        _selectedItem match {
                            case Some(idx) if idx >= index =>
                                _selectedItem = Some(idx + 1)
                            case _ => // do nothing
                        }
                        items = newList
                        newList
                    }
                    allDimension = None // invalidate calculated dimension
                    if (tail.isEmpty) {
                        display.syncExec(() => { setProgress(false) })
                    } else {
                        display.syncExec(() => { redraw() })
                    }
                } else {
                    // tail.cancel()
                }
            }
        }
    }

    def addSelectListener(func: Option[(T, Int)] => Unit): Unit = {
        _listeners +:= func
    }

    def select(item: Option[(T, Int)]): Unit = {
        _selectedItem = item map { _._2 }
        _listeners foreach { f => f(item) }
        display.asyncExec(() => redraw())
    }

    def selectIndex(index: Option[Int]): Unit = {
        _selectedItem = index
        select(index map { idx => (items(idx), idx) })
    }

    addPaintListener(new PaintListener {
        override def paintControl(e: PaintEvent): Unit = {
            val gc = e.gc
            if (allDimension.isEmpty) {
                itemBoundsMap = Map()
                val dimension = new Point(0, 0)
                items.zipWithIndex foreach { itemIdx =>
                    val (item, index) = itemIdx
                    val d = item.dimension(gc)

                    val itemBound = new Rectangle(0, dimension.y, d.x, d.y)
                    itemBoundsMap += (itemIdx -> itemBound)

                    dimension.x = math.max(dimension.x, d.x)
                    dimension.y += d.y
                }
                allDimension = Some(dimension)
            }
            val contentDimension = allDimension.get

            val bounds = getBounds
            if (_scroll.x + bounds.width > contentDimension.x) {
                _scroll.x = contentDimension.x - bounds.width
            }
            if (_scroll.x < 0) {
                _scroll.x = 0
            }
            if (_scroll.y + bounds.height > contentDimension.y) {
                _scroll.y = contentDimension.y - bounds.height
            }
            if (_scroll.y < 0) {
                _scroll.y = 0
            }

            itemBoundsMap foreach { itemBound =>
                val ((item, idx), bound) = itemBound

                if (bound.y < bounds.height || (bound.y + bound.height) > bounds.y) {
                    if (bound.x < bounds.width || (bound.x + bound.width) > bounds.x) {
                        val isSelected = _selectedItem contains idx
                        val scrolledBound = new Rectangle(bound.x - _scroll.x, bound.y - _scroll.y, bound.width, bound.height)
                        if (scrolledBound.width < 0) scrolledBound.width = getBounds.width
                        item.draw(gc, scrolledBound, isSelected)
                    }
                }
            }

            if (_progress) {
                gc.setBackground(SortedList.baseBackgroundColor)
                gc.drawText("Loading...", 0, 0)
            }
        }
    })

    private def refineRectangle(rect: Rectangle): Rectangle = {
        if (rect.width < 0) new Rectangle(rect.x, rect.y, getBounds.width, rect.height) else rect
    }

    addMouseListener(new MouseListener {
        override def mouseUp(e: MouseEvent): Unit = {}

        override def mouseDoubleClick(e: MouseEvent): Unit = {}

        override def mouseDown(e: MouseEvent): Unit = {
            val p = new Point(e.x + _scroll.x, e.y + _scroll.y)
            select(itemBoundsMap find { item => refineRectangle(item._2).contains(p) } map { _._1 })
        }
    })

    addMouseWheelListener(new MouseWheelListener {
        override def mouseScrolled(e: MouseEvent): Unit = {
            _scroll.y -= e.count * 5
            redraw()
        }
    })

    addKeyListener(new KeyListener {
        override def keyPressed(e: KeyEvent): Unit = {
            e.keyCode match {
                case SWT.ARROW_DOWN =>
                    _selectedItem match {
                        case Some(idx) if idx + 1 < items.length => selectIndex(Some(idx + 1))
                        case _ => // do nothing
                    }
                case SWT.ARROW_UP =>
                    _selectedItem match {
                        case Some(idx) if idx > 0 => selectIndex(Some(idx - 1))
                        case _ => // do nothing
                    }
                case _ => // do nothing
            }
        }

        override def keyReleased(e: KeyEvent): Unit = {}
    })
}
