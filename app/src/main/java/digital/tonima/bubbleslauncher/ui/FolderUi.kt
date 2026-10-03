package digital.tonima.bubbleslauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import digital.tonima.bubbleslauncher.R
import digital.tonima.bubbleslauncher.model.AppFolder
import digital.tonima.bubbleslauncher.model.AppInfo
import digital.tonima.bubbleslauncher.model.FolderOps

private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Rounded tile with up to four mini icons, like a phone home-screen folder. */
@Composable
fun FolderTile(section: FolderSection, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val preview = section.apps.take(4)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                preview.chunked(2).forEach { rowApps ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rowApps.forEach { app ->
                            val icon = rememberAppIcon(app)
                            Box(Modifier.size(26.dp)) {
                                icon?.let { Image(bitmap = it, contentDescription = null, colorFilter = Grayscale) }
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = section.folder.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(top = 6.dp)
                .size(width = 88.dp, height = 20.dp)
        )
    }
}

/** The contents of a folder: tap launches, long-press offers removal, header renames/deletes. */
@Composable
fun FolderDialog(
    section: FolderSection,
    onDismiss: () -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveApp: (AppInfo) -> Unit
) {
    var renaming by remember { mutableStateOf(false) }
    var draft by remember(section.folder.name) { mutableStateOf(section.folder.name) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (renaming) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = FolderOps.sanitizeName(it) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onRename(draft)
                                renaming = false
                            },
                            enabled = draft.isNotBlank()
                        ) { Text(stringResource(id = R.string.folder_save)) }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = section.folder.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { renaming = true }) {
                            Text(stringResource(id = R.string.folder_rename))
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(88.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    items(section.apps, key = { it.packageName }) { app ->
                        FolderAppCell(app = app, onLaunch = { onLaunch(app) }, onRemove = { onRemoveApp(app) })
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDelete) {
                        Text(
                            stringResource(id = R.string.folder_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.folder_close)) }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderAppCell(app: AppInfo, onLaunch: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val icon = rememberAppIcon(app)
    Column(
        modifier = Modifier
            .padding(6.dp)
            .combinedClickable(onClick = onLaunch, onLongClick = { menu = true }),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(52.dp)) {
            icon?.let { Image(bitmap = it, contentDescription = app.appName, colorFilter = Grayscale) }
        }
        Text(
            text = app.appName,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.menu_remove_from_folder)) },
                onClick = {
                    menu = false
                    onRemove()
                }
            )
        }
    }
}

/** Pick an existing folder or create a new one for [app]. */
@Composable
fun FolderPickerDialog(
    folders: List<AppFolder>,
    onPick: (AppFolder) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.folder_picker_title),
                    style = MaterialTheme.typography.titleLarge
                )
                folders.forEach { folder ->
                    TextButton(onClick = { onPick(folder) }, modifier = Modifier.fillMaxWidth()) {
                        Text(folder.name, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    }
                }
                if (folders.isNotEmpty()) HorizontalDivider()
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = FolderOps.sanitizeName(it) },
                    placeholder = { Text(stringResource(id = R.string.folder_new_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onCreate(name) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(id = R.string.folder_create)) }
            }
        }
    }
}
