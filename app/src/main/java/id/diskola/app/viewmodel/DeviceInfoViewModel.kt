package id.diskola.app.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import id.diskola.app.utils.DeviceInfoGroup
import id.diskola.app.utils.DeviceInfoReader
import id.diskola.app.utils.DeviceInfoRules
import javax.inject.Inject

/** "Tentang Perangkat" (Akun): a snapshot taken when the screen opens — RAM and free storage are the values of that moment. */
@HiltViewModel
class DeviceInfoViewModel @Inject constructor(reader: DeviceInfoReader) : ViewModel() {

    val groups: List<DeviceInfoGroup> = reader.read()

    /** What "Salin informasi" copies: exactly the rows on screen, plus when they were read. */
    val plainText: String = DeviceInfoRules.toPlainText(groups, reader.capturedAt())
}
