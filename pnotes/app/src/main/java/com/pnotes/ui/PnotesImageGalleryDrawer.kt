package com.pnotes.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pnotes.ui.icons.PnotesIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeviceMediaImage(
    val id: Long,
    val uri: Uri,
    val dateAdded: Long,
)

/**
 * Side Vertical Gallery Bar (Image Picker Drawer).
 *
 * Provides instant Apple-style slide-in access to recent photos on device,
 * quick camera capture, and system file browser integration.
 */
@Composable
fun PnotesImageGalleryDrawer(
    onSelectImageBytes: (ByteArray) -> Unit,
    onLaunchFilePicker: () -> Unit,
    onLaunchCamera: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = isGlassDark()
    val scope = rememberCoroutineScope()
    val recentImages = remember { mutableStateListOf<DeviceMediaImage>() }
    var isLoading by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(hasImagePermission(context)) }

    fun loadImages() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            val list = queryRecentDeviceImages(context, limit = 50)
            withContext(Dispatchers.Main) {
                recentImages.clear()
                recentImages.addAll(list)
                isLoading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        hasPermission = granted
        if (granted) {
            loadImages()
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            loadImages()
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .width(230.dp)
            .fillMaxHeight()
            .glassmorphic(
                shape = androidx.compose.ui.graphics.RectangleShape,
                elevation = 16.dp,
                borderWidth = 1.2.dp,
                isDark = isDark,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ==========================================
            // 1. HEADER: Icon + "PHOTOS & GALLERY" + Close
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = PnotesIcons.image,
                        contentDescription = "Gallery",
                        tint = if (isDark) Color.White else Color(0xFF09090B),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "PHOTOS & GALLERY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                        letterSpacing = 0.55.sp,
                    )
                }

                // Close Button ("X")
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE4E4E7), RoundedCornerShape(6.dp))
                        .bouncyClickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.close,
                        contentDescription = "Close",
                        tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF09090B),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            // ==========================================
            // 2. QUICK ACTION BUTTONS: Browse Files & Camera
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Browse Storage Files
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF222228) else Color(0xFFF4F4F5))
                        .border(0.6.dp, if (isDark) Color(0xFF2E2E36) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                        .bouncyClickable(onClick = onLaunchFilePicker)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.folder,
                        contentDescription = "Browse",
                        tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF3F3F46),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Browse",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF4F4F6) else Color(0xFF18181B),
                    )
                }

                // Take Photo / Camera
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF222228) else Color(0xFFF4F4F5))
                        .border(0.6.dp, if (isDark) Color(0xFF2E2E36) else Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                        .bouncyClickable(onClick = onLaunchCamera)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = PnotesIcons.image,
                        contentDescription = "Camera",
                        tint = if (isDark) Color(0xFFD4D4D8) else Color(0xFF3F3F46),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Camera",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFF4F4F6) else Color(0xFF18181B),
                    )
                }
            }

            // ==========================================
            // 3. PERMISSION PROMPT OR RECENT PHOTOS GRID
            // ==========================================
            if (!hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x22FFFFFF) else Color(0x10000000)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = PnotesIcons.image,
                                contentDescription = null,
                                tint = if (isDark) Color.White else Color(0xFF09090B),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            text = "Photos Access",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF09090B),
                        )
                        Text(
                            text = "Allow access to easily select and insert photos into your notes.",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color.White else Color(0xFF09090B))
                                .clickable {
                                    permissionLauncher.launch(getImagePermissions())
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Allow Access",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF09090B) else Color.White,
                            )
                        }
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = if (isDark) Color.White else Color(0xFF09090B),
                    )
                }
            } else if (recentImages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = PnotesIcons.image,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA),
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = "No Recent Photos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                        )
                        Text(
                            text = "Tap Browse to pick images from storage.",
                            fontSize = 10.5.sp,
                            color = if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(recentImages, key = { it.id }) { item ->
                        GalleryThumbnailCard(
                            item = item,
                            isDark = isDark,
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val bytes = runCatching {
                                        context.contentResolver.openInputStream(item.uri)?.use { it.readBytes() }
                                    }.getOrNull()
                                    if (bytes != null) {
                                        withContext(Dispatchers.Main) {
                                            onSelectImageBytes(bytes)
                                            onClose()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryThumbnailCard(
    item: DeviceMediaImage,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) {
            loadThumbnailBitmap(context, item.uri)
        }
    }

    val cardShape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(2.dp, cardShape, spotColor = if (isDark) Color.Black else Color(0x1F000000))
            .clip(cardShape)
            .background(if (isDark) Color(0xFF1B1B20) else Color(0xFFE4E4E7))
            .border(0.8.dp, if (isDark) Color(0x26FFFFFF) else Color(0x18000000), cardShape)
            .bouncyClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Photo Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = PnotesIcons.image,
                contentDescription = null,
                tint = if (isDark) Color(0xFF3F3F46) else Color(0xFFD4D4D8),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun hasImagePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun getImagePermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun loadThumbnailBitmap(context: Context, uri: Uri): ImageBitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(200, 200), null).asImageBitmap()
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        }
    }.getOrNull()
}

private fun queryRecentDeviceImages(context: Context, limit: Int = 50): List<DeviceMediaImage> {
    val results = mutableListOf<DeviceMediaImage>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED,
    )
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    runCatching {
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            var count = 0
            while (it.moveToNext() && count < limit) {
                val id = it.getLong(idColumn)
                val date = it.getLong(dateColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                results.add(DeviceMediaImage(id, contentUri, date))
                count++
            }
        }
    }
    return results
}
