
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mytaskpro.util.ImageUtils

@Composable
fun ImagePicker(onImagePicked: (Uri?) -> Unit) {
    val context = LocalContext.current
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri.value = uri
        onImagePicked(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            onImagePicked(imageUri.value)
        }
    }

    Column {
        Button(onClick = { galleryLauncher.launch("image/*") }) {
            Text("Choose from Gallery")
        }
        Button(onClick = {
            val uri = ImageUtils.createImageUri(context)
            imageUri.value = uri
            cameraLauncher.launch(uri)
        }) {
            Text("Take Photo")
        }
    }
}