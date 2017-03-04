package com.giyeok.passzero.ui.swt

import com.giyeok.passzero.Session
import com.giyeok.passzero.ui.Config
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control

class MainUI(parent: Composite, style: Int, config: Config) extends Composite(parent, style) {
    // 1. localInfo 파일이 있는지 확인한다
    // 2. 없으면 계정 설정 화면으로 간다. 계정 설정이 완료되면 비밀번호 입력 화면으로 간다
    // 3. 있으면 비밀번호 입력 화면으로 간다
    // 4. 비밀번호가 입력되면 localInfo를 로드한다. 로드하는 도중 오류가 발생하면 다시 반복한다.
    //    별도로 비밀번호가 틀린 것을 검증할 방법은 없고 localInfo 로드가 실패하면 비밀번호가 틀렸거나 localInfo 파일이 손상된 것으로 본다
    // 5. localInfo 로드가 완료되면 session을 생성해서 비밀번호 목록 화면으로 간다.
    // 6. system tray에도 아이콘을 표시하고, 트레이 아이콘을 누르면 UI가 화면에 나온다
    // - MainUI의 레이아웃은 FillLayout으로 하고 상태가 변경되면 기존의 컨트롤은 dispose한다

    setLayout(new FillLayout)

    def replaceChild(contentFunc: MainUI => Control): Unit = {
        getChildren foreach { _.dispose() }
        contentFunc(this)
        requestLayout()
    }

    def init(): Unit = {
        if (!config.localInfoFile.exists()) {
            replaceChild(new InitializationUI(getShell, _, SWT.NONE, config))
        } else {
            replaceChild(new MasterPasswordUI(getShell, _, SWT.NONE, config))
        }
    }

    def sessionInitialized(session: Session): Unit = {
        replaceChild(new PasswordListUI(_, SWT.NONE, session, config))
    }

    init()
}
