//package com.giyeok.passzero.ui.swt
//
//import java.util.concurrent.atomic.AtomicLong
//import scala.concurrent.ExecutionContext
//import scala.concurrent.Future
//import com.giyeok.passzero.utils.FutureStream
//import org.eclipse.swt.SWT
//import org.eclipse.swt.events.KeyEvent
//import org.eclipse.swt.events.KeyListener
//import org.eclipse.swt.events.MouseEvent
//import org.eclipse.swt.events.MouseListener
//import org.eclipse.swt.events.MouseWheelListener
//import org.eclipse.swt.events.PaintEvent
//import org.eclipse.swt.events.PaintListener
//import org.eclipse.swt.graphics.Color
//import org.eclipse.swt.graphics.GC
//import org.eclipse.swt.graphics.Point
//import org.eclipse.swt.graphics.Rectangle
//import org.eclipse.swt.widgets.Canvas
//import org.eclipse.swt.widgets.Composite
//import org.eclipse.swt.widgets.Display
//
//trait SortedListItem {
//    def >(other: SortedListItem): Boolean // = a > b
//    def dimension(gc: GC): Point
//    def draw(gc: GC, bounds: Rectangle, selected: Boolean): Unit
//}
//
//case class TextSortedListItem[T](data: T, text: String) extends SortedListItem {
//    def >(other: SortedListItem): Boolean = other match {
//        case otherItem: TextSortedListItem[_] => text.toLowerCase > otherItem.text.toLowerCase
//    }
//    def dimension(gc: GC): Point = {
//        val p = gc.textExtent(text)
//        p.x *= -1
//        p.y += 4
//        p
//    }
//    def draw(gc: GC, bounds: Rectangle, selected: Boolean): Unit = {
//        if (selected) {
//            gc.setBackground(SortedList.selectedBackgroundColor)
//            gc.fillRectangle(bounds)
//        } else {
//            gc.setBackground(SortedList.baseBackgroundColor)
//        }
//        gc.drawText(text, bounds.x + 4, bounds.y + 2)
//        if (selected) {
//            gc.setForeground(SortedList.selectedBorderColor)
//            gc.drawRectangle(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1)
//            gc.setForeground(SortedList.baseForegroundColor)
//        }
//    }
//}
//
//object SortedList {
//    val selectedBackgroundColor = new Color(null, 255, 255, 0)
//    val selectedBorderColor = new Color(null, 255, 0, 0)
//    val baseBackgroundColor = new Color(null, 255, 255, 255)
//    val baseForegroundColor = new Color(null, 0, 0, 0)
//}
//
//// TODO 나중에 StructuredTextView로 바꾸는 것을 고려
//class SortedList[I, T <: SortedListItem](display: Display, parent: Composite, style: Int, source: I => Future[T]) extends Canvas(parent, style) {
//    private var items = Seq[(I, T)]()
//    private var idToIndex = Map[I, Int]()
//    private var idToItem = Map[I, T]()
//
//    private var allDimension: Option[Point] = None
//    private var itemBoundsMap = Map[I, (T, Rectangle)]()
//
//    private var _selectedId = Option.empty[I]
//    def selectedId: Option[I] = _selectedId
//
//    private val sourceIdCounter = new AtomicLong(0)
//    private def newSourceId(): Long = sourceIdCounter.incrementAndGet()
//
//    private var _progress: Boolean = false
//    def setProgress(progress: Boolean): Unit = {
//        _progress = progress
//        redrawIfNotDisposed()
//    }
//
//    def redrawIfNotDisposed(): Unit = {
//        if (!this.isDisposed) {
//            redraw()
//        }
//    }
//
//    private val _scroll: Point = new Point(0, 0)
//    private var showSelected: Boolean = false
//
//    private var _listeners: Seq[(Option[(I, T)], Option[Point]) => Unit] = Seq()
//
//    setBackground(SortedList.baseBackgroundColor)
//
//    def clear(): Unit = {
//        items = Seq()
//        idToIndex = Map()
//        idToItem = Map()
//        allDimension = None
//        redrawIfNotDisposed()
//    }
//
//    def addItem(id: I, item: T, needsRedraw: Boolean): Int = {
//        val index = items.zipWithIndex find { p => p._1._2 > item } map { _._2 } getOrElse items.length
//        val (init, tail) = items.splitAt(index)
//        val newList: Seq[(I, T)] = init ++ ((id, item) +: tail)
//        items = newList
//        idToIndex = (items.zipWithIndex map { p => p._1._1 -> p._2 }).toMap
//        idToItem += (id -> item)
//        if (needsRedraw) {
//            allDimension = None
//            redrawIfNotDisposed()
//        }
//        index
//    }
//
//    //    def removeItem(idx: Int, needsRedraw: Boolean = true): Unit = {
//    // TODO 현재 선택된 아이템이 제거되는 경우엔 선택 None으로
//    //        if (_selectedItem contains idx) {
//    //            _selectedItem = None
//    //        }
//    //        items = (items take idx) ++ (items drop (idx + 1))
//    //        if (needsRedraw) {
//    //            allDimension = None
//    //            redrawIfNotDisposed()
//    //        }
//    //    }
//
//    //    def replaceItem(idx: Int, newItem: T): Unit = {
//    // TODO 현재 선택된 아이템이 바뀌는 경우에도 listener는 호출하지 않음
//    // 순서는 유지해야 함
//    //        val wasSelected = _selectedItem contains idx
//    //        items = (items take idx) ++ (items drop (idx + 1))
//    //        val newIndex = addItem(newItem, needsRedraw = false)
//    //        if (wasSelected) {
//    //            _selectedItem = Some(newIndex)
//    //        }
//    //        allDimension = None
//    //        showSelected = true
//    //        redrawIfNotDisposed()
//    //    }
//
//    //    def replaceItem(id: String, newItem: T): Unit = {
//    //        val idx = items map { _.id } indexOf id
//    //        if (idx < 0) {
//    //            ???
//    //        } else {
//    //            replaceItem(idx, newItem)
//    //        }
//    //    }
//
//    def setSource(stream: FutureStream[Seq[(I, T)]], finishedListener: () => Unit): Unit = {
//        clear()
//        setProgress(true)
//        val currentSourceId = newSourceId()
//        stream foreach { (page, tail) =>
//            this.synchronized {
//                if (this.sourceIdCounter.get() == currentSourceId) {
//                    page foreach { p => addItem(p._1, p._2, needsRedraw = false) }
//                    allDimension = None // invalidate calculated dimension
//                    if (tail.isEmpty) {
//                        display.syncExec(() => {
//                            setProgress(false)
//                            finishedListener()
//                        })
//                    } else {
//                        display.syncExec(() => { redrawIfNotDisposed() })
//                    }
//                } else {
//                    // tail.cancel()
//                }
//            }
//        }
//    }
//
//    def addSelectListener(func: (Option[(I, T)], Option[Point]) => Unit): Unit = {
//        _listeners +:= func
//    }
//
//    def select(id: Option[I], pointOpt: Option[Point]): Unit = {
//        _selectedId = id
//        _listeners foreach { f => f(id map { i => (i, idToItem(i)) }, pointOpt) }
//        showSelected = true
//        display.asyncExec(() => redrawIfNotDisposed())
//    }
//
//    addPaintListener(new PaintListener {
//        override def paintControl(e: PaintEvent): Unit = {
//            val gc = e.gc
//            if (allDimension.isEmpty) {
//                itemBoundsMap = Map()
//                val dimension = new Point(0, 0)
//                items foreach { idItem =>
//                    val (id, item) = idItem
//                    val d = item.dimension(gc)
//
//                    val itemBound = new Rectangle(0, dimension.y, d.x, d.y)
//                    itemBoundsMap += (id -> (item, itemBound))
//
//                    dimension.x = math.max(dimension.x, d.x)
//                    dimension.y += d.y
//                }
//                allDimension = Some(dimension)
//            }
//            val contentDimension = allDimension.get
//
//            val bounds = getBounds
//
//            if (showSelected) {
//                _selectedId foreach { selected =>
//                    val (item, bound) = itemBoundsMap(selected)
//                    if (bound.y + bound.height - _scroll.y > bounds.height) {
//                        _scroll.y = (bound.y + bound.height) - bounds.height
//                    }
//                    if (bound.y - _scroll.y < 0) {
//                        _scroll.y = bound.y
//                    }
//                }
//                showSelected = false
//            }
//
//            if (_scroll.x + bounds.width > contentDimension.x) {
//                _scroll.x = contentDimension.x - bounds.width
//            }
//            if (_scroll.x < 0) {
//                _scroll.x = 0
//            }
//            if (_scroll.y + bounds.height > contentDimension.y) {
//                _scroll.y = contentDimension.y - bounds.height
//            }
//            if (_scroll.y < 0) {
//                _scroll.y = 0
//            }
//
//            itemBoundsMap foreach { itemBound =>
//                val (id, (item, bound)) = itemBound
//
//                if (bound.y < bounds.height || (bound.y + bound.height) > bounds.y) {
//                    if (bound.x < bounds.width || (bound.x + bound.width) > bounds.x) {
//                        val isSelected = _selectedId contains id
//                        val scrolledBound = new Rectangle(bound.x - _scroll.x, bound.y - _scroll.y, bound.width, bound.height)
//                        if (scrolledBound.width < 0) scrolledBound.width = getBounds.width
//                        item.draw(gc, scrolledBound, isSelected)
//                    }
//                }
//            }
//
//            if (_progress) {
//                gc.setBackground(SortedList.baseBackgroundColor)
//                gc.drawText("Loading...", 0, 0)
//            }
//        }
//    })
//
//    private def refineRectangle(rect: Rectangle): Rectangle = {
//        if (rect.width < 0) new Rectangle(rect.x, rect.y, getBounds.width, rect.height) else rect
//    }
//
//    addMouseListener(new MouseListener {
//        override def mouseUp(e: MouseEvent): Unit = {}
//
//        override def mouseDoubleClick(e: MouseEvent): Unit = {}
//
//        override def mouseDown(e: MouseEvent): Unit = {
//            val p = new Point(e.x + _scroll.x, e.y + _scroll.y)
//            val selected = itemBoundsMap find { item => refineRectangle(item._2._2).contains(p) }
//            select(selected map { _._1 }, selected map { x => new Point(p.x - x._2._2.x, p.y - x._2._2.y) })
//        }
//    })
//
//    addMouseWheelListener(new MouseWheelListener {
//        override def mouseScrolled(e: MouseEvent): Unit = {
//            _scroll.y -= e.count * 5
//            redrawIfNotDisposed()
//        }
//    })
//
//    addKeyListener(new KeyListener {
//        override def keyPressed(e: KeyEvent): Unit = {
//            e.keyCode match {
//                case SWT.ARROW_DOWN =>
//                    _selectedId match {
//                        case Some(id) if idToIndex(id) + 1 < items.length =>
//                            select(Some(items(idToIndex(id) + 1)._1), None)
//                        case _ => // do nothing
//                    }
//                case SWT.ARROW_UP =>
//                    _selectedId match {
//                        case Some(id) if idToIndex(id) > 0 =>
//                            select(Some(items(idToIndex(id) - 1)._1), None)
//                        case _ => // do nothing
//                    }
//                case SWT.HOME =>
//                    _selectedId match {
//                        case Some(_) =>
//                            select(Some(items(0)._1), None)
//                        case _ => // do nothing
//                    }
//                case SWT.END =>
//                    _selectedId match {
//                        case Some(_) =>
//                            select(Some(items.last._1), None)
//                        case _ => // do nothing
//                    }
//                case _ => // do nothing
//            }
//        }
//
//        override def keyReleased(e: KeyEvent): Unit = {}
//    })
//
//    private def allRedraw(): Unit = {
//        allDimension = None
//        itemBoundsMap = Map()
//        redrawIfNotDisposed()
//    }
//
//    def refreshAllItems(): Unit = {
//        // TODO Future가 완료되는대로 replace하는 식으로 수정해서 깜빡임 없애기
//        implicit val ec = ExecutionContext.global
//
//        val oldItems = items
//        items = Seq()
//
//        setProgress(true)
//        allRedraw()
//        val futures = oldItems map { x => (x._1, source(x._1)) } map { pair =>
//            val (id, future) = pair
//            future foreach { item =>
//                display.asyncExec(() => addItem(id, item, needsRedraw = true))
//            }
//            future
//        }
//        Future.sequence(futures) foreach { _ =>
//            display.syncExec(() => setProgress(false))
//        }
//    }
//}
