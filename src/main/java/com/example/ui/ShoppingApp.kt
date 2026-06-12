package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.viewmodel.Screen
import com.example.viewmodel.ShoppingViewModel
import java.text.SimpleDateFormat
import java.util.*

// --- Custom Supermarket Tag Configuration ---
data class TagConfig(val label: String, val emoji: String, val colorLight: Color, val colorDark: Color)

val TAGS_LIST = listOf(
    TagConfig("Alimentación", "🥫", Color(0xFFFFF3E0), Color(0xFF4E3629)),
    TagConfig("Frutería", "🍎", Color(0xFFE8F5E9), Color(0xFF1B3B22)),
    TagConfig("Carnicería", "🥩", Color(0xFFFFEBEE), Color(0xFF4A151B)),
    TagConfig("Pescadería", "🐟", Color(0xFFE3F2FD), Color(0xFF0D324D)),
    TagConfig("Lácteos", "🥛", Color(0xFFFFFDE7), Color(0xFF3E3B1C)),
    TagConfig("Congelados", "❄️", Color(0xFFE0F7FA), Color(0xFF00363A)),
    TagConfig("Limpieza", "🧼", Color(0xFFF3E5F5), Color(0xFF381A3C)),
    TagConfig("Perfumería", "🧴", Color(0xFFEDE7F6), Color(0xFF2A1B40)),
    TagConfig("Bebidas", "🥤", Color(0xFFECEFF1), Color(0xFF263238)),
    TagConfig("Panadería", "🍞", Color(0xFFFFF8E1), Color(0xFF4E3E20)),
    TagConfig("Mascotas", "🐶", Color(0xFFEFEBE9), Color(0xFF3E2723)),
    TagConfig("Otros", "📦", Color(0xFFF5F5F5), Color(0xFF303030))
)

val LocalCustomTags = staticCompositionLocalOf<List<CustomTag>> { emptyList() }

fun getTagConfig(tagName: String, customTags: List<CustomTag> = emptyList()): TagConfig {
    val foundCustom = customTags.find { it.name.equals(tagName, ignoreCase = true) }
    if (foundCustom != null) {
        val lightColor = try { Color(android.graphics.Color.parseColor(foundCustom.colorLightHex)) } catch (e: Exception) { Color(0xFFFFF3E0) }
        val darkColor = try { Color(android.graphics.Color.parseColor(foundCustom.colorDarkHex)) } catch (e: Exception) { Color(0xFF4E3629) }
        return TagConfig(foundCustom.name, foundCustom.emoji, lightColor, darkColor)
    }
    val foundHardcoded = TAGS_LIST.find { it.label.equals(tagName, ignoreCase = true) }
    if (foundHardcoded != null) return foundHardcoded
    return TagConfig(tagName, "🏷️", Color(0xFFF5F5F5), Color(0xFF303030))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingApp(viewModel: ShoppingViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val customTags by viewModel.allCustomTags.collectAsState()

    CompositionLocalProvider(LocalCustomTags provides customTags) {
        // Main scaffold providing full-screen boundaries with animated screens
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "screen_navigation"
                ) { screen ->
                    when (screen) {
                        is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                        is Screen.ListEditor -> ListEditorScreen(
                            viewModel = viewModel,
                            listId = screen.listId,
                            listName = screen.listName
                        )
                        is Screen.Planning -> PlanningScreen(
                            viewModel = viewModel,
                            listId = screen.listId,
                            listName = screen.listName
                        )
                        is Screen.SupermarketMode -> SupermarketModeScreen(
                            viewModel = viewModel,
                            listId = screen.listId,
                            listName = screen.listName
                        )
                        is Screen.HistoryDetails -> HistoryDetailsScreen(
                            viewModel = viewModel,
                            groupId = screen.groupId,
                            listName = screen.listName,
                            timestamp = screen.timestamp,
                            total = screen.total
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN: DASHBOARD (LISTS & HISTORIC RECEIPTS)
// ==========================================
@Composable
fun DashboardScreen(viewModel: ShoppingViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val lists by viewModel.allLists.collectAsState()
    val historyGroups by viewModel.allHistoryGroups.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAuthorDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Elegant Eco Header Dashboard Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(extraLargePaddingHeader()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mis Compras",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Organiza y controla tus gastos",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("btn_backup_restore")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Copia de seguridad",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showAuthorDialog = true },
                        modifier = Modifier.testTag("btn_about_author")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información del autor",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Tabs for Lists vs History Receipts
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mis Listas", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_lists")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Historial", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_history")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Display
            if (selectedTab == 0) {
                if (lists.isEmpty()) {
                    EmptyStatePlaceholder(
                        emoji = "🛒",
                        title = "¿No hay listas creadas?",
                        subtitle = "Crea tu primera lista (ej. Mercadona, Carrefour) usando el botón + de abajo."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(lists, key = { it.id }) { shoppingList ->
                            ShoppingListCard(
                                shoppingList = shoppingList,
                                onEditProducts = {
                                    viewModel.selectList(shoppingList.id)
                                    viewModel.navigateTo(Screen.ListEditor(shoppingList.id, shoppingList.name))
                                },
                                onGoShopping = {
                                    viewModel.prepareAndStartPlanning(shoppingList.id)
                                    viewModel.navigateTo(Screen.Planning(shoppingList.id, shoppingList.name))
                                },
                                onDelete = { viewModel.deleteList(shoppingList) },
                                onRename = { newName -> viewModel.updateListName(shoppingList.id, newName) }
                            )
                        }
                    }
                }
            } else {
                if (historyGroups.isEmpty()) {
                    EmptyStatePlaceholder(
                        emoji = "🧾",
                        title = "Historial vacío",
                        subtitle = "Tus compras completadas aparecerán aquí con su recibo detallado."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(historyGroups, key = { it.id }) { group ->
                            HistoryGroupCard(
                                group = group,
                                onClick = {
                                    viewModel.selectHistoryGroup(group.id)
                                    viewModel.navigateTo(
                                        Screen.HistoryDetails(
                                            groupId = group.id,
                                            listName = group.listName,
                                            timestamp = group.timestamp,
                                            total = group.totalAmount
                                        )
                                    )
                                },
                                onDelete = { viewModel.deleteHistoryGroup(group.id) }
                            )
                        }
                    }
                }
            }
        }

        // FAB to add List
        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("create_list_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear nueva lista")
            }
        }

        // Dialog for creation
        if (showCreateDialog) {
            CreateListDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name ->
                    viewModel.createList(name)
                    showCreateDialog = false
                }
            )
        }

        // Dialog for Author details
        if (showAuthorDialog) {
            Dialog(onDismissRequest = { showAuthorDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circle Avatar Icon representation
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Sobre el Autor",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Detail items
                        AuthorDetailRow(
                            icon = Icons.Default.Person,
                            label = "Nombre",
                            value = "Francisco Javier Bernal D."
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

                        AuthorDetailRow(
                            icon = Icons.Default.Email,
                            label = "Correo electrónico",
                            value = "fjbernal2020@gmail.com"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

                        AuthorDetailRow(
                            icon = Icons.Default.Place,
                            label = "Ciudad",
                            value = "Málaga, España"
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = { showLicenseDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Licencia de uso", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showAuthorDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Cerrar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Dialog for License
        if (showLicenseDialog) {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Dialog(onDismissRequest = { showLicenseDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Licencia de Uso",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Copyright (c) 2026\nFrancisco Javier Bernal Domínguez\n\n" +
                                   "Licencia: GPL-3.0\n\n" +
                                   "Este software se proporciona \"tal cual\", sin garantía de ningún tipo, " +
                                   "expresa o implícita. El autor no será responsable de daños, pérdidas de " +
                                   "datos, pérdidas económicas o cualquier consecuencia derivada del uso del " +
                                   "software, en la máxima medida permitida por la ley aplicable.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = {
                                try {
                                    uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.en.html")
                                } catch (e: Exception) {
                                    // Fallback if no web browser
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("btn_license_link")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ver Licencia GPL-3.0 en la Web",
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showLicenseDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Aceptar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showBackupDialog) {
            BackupRestoreDialog(
                onDismissRequest = { showBackupDialog = false },
                viewModel = viewModel
            )
        }
    }
}

// Placeholder for empty lists
@Composable
fun EmptyStatePlaceholder(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// Shopping list row element CARD
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingListCard(
    shoppingList: ShoppingList,
    onEditProducts: () -> Unit,
    onGoShopping: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onGoShopping,
                onLongClick = { showRenameDialog = true }
            )
            .testTag("list_card_${shoppingList.name}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shoppingList.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toca para ir a comprar",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Design List (Catalog editor)
                IconButton(
                    onClick = onEditProducts,
                    modifier = Modifier.testTag("btn_edit_${shoppingList.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar items",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete entire list
                IconButton(
                    onClick = { showConfirmDelete = true },
                    modifier = Modifier.testTag("btn_delete_${shoppingList.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar lista",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameListDialog(
            currentName = shoppingList.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                onRename(newName)
                showRenameDialog = false
            }
        )
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Eliminar lista") },
            text = { Text("¿Seguro que deseas eliminar la lista de '${shoppingList.name}'? Esto borrará también sus productos guardados.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Past receipt history row element card
@Composable
fun HistoryGroupCard(
    group: PurchaseHistoryGroup,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    val formattedDate = remember(group.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(group.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("history_card_${group.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.listName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.2f €", group.totalAmount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Total Pagado",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            IconButton(onClick = { showConfirmDelete = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Borrar del historial",
                    tint = Color.Gray
                )
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Eliminar del historial") },
            text = { Text("¿Deseas borrar por completo este ticket de compra del historial?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


// ==========================================
// SCREEN: LIST EDITOR (CREATE / EDIT PRODUCTS CATALOG)
// ==========================================
@Composable
fun ListEditorScreen(
    viewModel: ShoppingViewModel,
    listId: Int,
    listName: String
) {
    val products by viewModel.currentTemplateProducts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<TemplateProduct?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // App header with Back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(extraLargePaddingHeader()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Dashboard) },
                modifier = Modifier.testTag("btn_back")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Artículos de $listName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Configura tu plantilla de productos",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("btn_add_product")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir producto",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (products.isEmpty()) {
            EmptyStatePlaceholder(
                emoji = "🥕",
                title = "Plantilla vacía",
                subtitle = "Haz clic en el botón '+' para añadir productos que sueles comprar habitualmente en $listName."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    TemplateProductCard(
                        product = product,
                        onClick = { productToEdit = product },
                        onDelete = { viewModel.deleteTemplateProduct(product) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditProductDialog(
            title = "Añadir Producto a $listName",
            onDismiss = { showAddDialog = false },
            onSave = { name, quantity, tag, price ->
                viewModel.addTemplateProduct(listId, name, quantity, tag, price)
                showAddDialog = false
            },
            onDeleteTag = { viewModel.deleteCustomTag(it) }
        )
    }

    if (productToEdit != null) {
        val innerProduct = productToEdit!!
        AddEditProductDialog(
            title = "Editar Producto",
            initialName = innerProduct.name,
            initialQuantity = innerProduct.quantity,
            initialTag = innerProduct.tag,
            initialPrice = innerProduct.lastPrice,
            onDismiss = { productToEdit = null },
            onSave = { name, quantity, tag, price ->
                viewModel.updateTemplateProduct(
                    innerProduct.copy(
                        name = name,
                        quantity = quantity,
                        tag = tag,
                        lastPrice = price
                    )
                )
                productToEdit = null
            },
            onDeleteTag = { viewModel.deleteCustomTag(it) }
        )
    }
}

@Composable
fun TemplateProductCard(
    product: TemplateProduct,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val customTags = LocalCustomTags.current
    val tagCfg = remember(product.tag, customTags) { getTagConfig(product.tag, customTags) }
    val isDark = isSystemInDarkTheme()
    val badgeBg = if (isDark) tagCfg.colorDark else tagCfg.colorLight
    val badgeFg = if (isDark) Color.White else tagCfg.colorLight.darken()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("product_template_${product.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Badge
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${tagCfg.emoji} ${product.tag}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeFg
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cantidad típica: ${product.quantity}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Último precio: " + String.format(Locale.US, "%.2f €", product.lastPrice),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar artículo",
                    tint = Color.Gray.copy(alpha = 0.6f)
                )
            }
        }
    }
}


// ==========================================
// SCREEN: SHOPPING TRIP PLANNING (PHASE 1)
// ==========================================
@Composable
fun PlanningScreen(
    viewModel: ShoppingViewModel,
    listId: Int,
    listName: String
) {
    val activeTripItems by viewModel.currentActiveTripItems.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(extraLargePaddingHeader()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Preparar compra para $listName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Selecciona qué necesitas comprar hoy",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        if (activeTripItems.isEmpty()) {
            EmptyStatePlaceholder(
                emoji = "🛒",
                title = "No hay productos en tu plantilla",
                subtitle = "Primero ve al diseñador de la lista (icono de lápiz) y añade productos para poder planificar una compra."
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                // Button to "Select All"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.selectAllActiveTripItems(listId) },
                        modifier = Modifier.testTag("btn_select_all")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Seleccionar todos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(activeTripItems, key = { it.id }) { item ->
                        PlanningItemCard(
                            item = item,
                            onToggle = {
                                viewModel.updateActiveTripItem(item.copy(isSelectedForTrip = !item.isSelectedForTrip))
                            },
                            onQuantityChange = { newQty ->
                                viewModel.updateActiveTripItem(item.copy(quantity = newQty))
                            }
                        )
                    }
                }

                // Iniciar Supermercado Mode Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    val count = activeTripItems.count { it.isSelectedForTrip }
                    Button(
                        onClick = {
                            viewModel.navigateTo(Screen.SupermarketMode(listId, listName))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_go_supermarket"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ir al Supermercado ($count artículos)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanningItemCard(
    item: ActiveTripItem,
    onToggle: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .testTag("planning_item_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelectedForTrip) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (item.isSelectedForTrip) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelectedForTrip,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("chk_plan_${item.name}")
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isSelectedForTrip) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    textDecoration = if (item.isSelectedForTrip) null else TextDecoration.LineThrough
                )
                Text(
                    text = item.tag,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Quantity adjusters if needed
            if (item.isSelectedForTrip) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = item.quantity.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}


// ==========================================
// SCREEN: IN-SUPERMARKET SHOPPING TRIP (PHASE 2)
// ==========================================
@Composable
fun SupermarketModeScreen(
    viewModel: ShoppingViewModel,
    listId: Int,
    listName: String
) {
    val rawItems by viewModel.currentActiveTripItems.collectAsState()
    val activeItems = remember(rawItems) { rawItems.filter { it.isSelectedForTrip } }

    var sortByTag by remember { mutableStateOf(false) } // False = Alphabetical, True = By Tag
    var showAddManualItemDialog by remember { mutableStateOf(false) }
    var itemForPriceUpdate by remember { mutableStateOf<ActiveTripItem?>(null) }

    // Computes totals
    val totalPlanned = remember(activeItems) { activeItems.sumOf { it.quantity * it.price } }
    val totalBought = remember(activeItems) { activeItems.filter { it.isBought }.sumOf { it.quantity * it.price } }

    // Sorted active elements
    val sortedItems = remember(activeItems, sortByTag) {
        if (sortByTag) {
            activeItems.sortedWith(compareBy({ it.tag }, { it.name }))
        } else {
            activeItems.sortedBy { it.name }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(extraLargePaddingHeader()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Planning(listId, listName)) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Atrás al planificador")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Comprando en $listName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Añade nuevos precios o items al vuelo",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Quick add product during shopping
            IconButton(onClick = { showAddManualItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Articulo imprevisto", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Sorting Segment Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { sortByTag = false },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("btn_sort_alpha"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!sortByTag) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!sortByTag) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Orden A-Z", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { sortByTag = true },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("btn_sort_tag"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sortByTag) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (sortByTag) Color.White else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Por Pasillo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeItems.isEmpty()) {
            EmptyStatePlaceholder(
                emoji = "🎒",
                title = "Carrito vacío de compras",
                subtitle = "No has seleccionado ningún producto para comprar. Vuelve e incluye al menos uno."
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(sortedItems, key = { it.id }) { item ->
                        SupermarketItemCard(
                            item = item,
                            onToggleBought = {
                                viewModel.updateActiveTripItem(item.copy(isBought = !item.isBought))
                            },
                            onEditPrice = { itemForPriceUpdate = item }
                        )
                    }
                }

                // BOTTOM SUMMARY BAR LOCKED
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = String.format(Locale.US, "%.2f €", totalBought),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Total comprado (Caja)", fontSize = 11.sp, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.2f €", totalPlanned),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text("Total Planificado", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fin vs Cancel Action bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelPurchase(listId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.error)
                            }

                            Button(
                                onClick = { viewModel.finalizePurchase(listId, listName) },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(44.dp)
                                    .testTag("btn_finalize_purchase"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finalizar Compra", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddManualItemDialog) {
        AddEditProductDialog(
            title = "Añadir Artículo imprevisto",
            onDismiss = { showAddManualItemDialog = false },
            onSave = { name, quantity, tag, price ->
                viewModel.createActiveTripItemManually(listId, name, quantity, tag, price)
                showAddManualItemDialog = false
            },
            onDeleteTag = { viewModel.deleteCustomTag(it) }
        )
    }

    if (itemForPriceUpdate != null) {
        val target = itemForPriceUpdate!!
        PriceUpdateDialog(
            itemName = target.name,
            currentPrice = target.price,
            onDismiss = { itemForPriceUpdate = null },
            onSave = { newPrice ->
                viewModel.updateActiveTripItem(target.copy(price = newPrice))
                itemForPriceUpdate = null
            }
        )
    }
}

@Composable
fun SupermarketItemCard(
    item: ActiveTripItem,
    onToggleBought: () -> Unit,
    onEditPrice: () -> Unit
) {
    val customTags = LocalCustomTags.current
    val tagCfg = remember(item.tag, customTags) { getTagConfig(item.tag, customTags) }
    val isDark = isSystemInDarkTheme()
    val badgeBg = if (isDark) tagCfg.colorDark else tagCfg.colorLight
    val badgeFg = if (isDark) Color.White else tagCfg.colorLight.darken()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggleBought)
            .testTag("supermarket_item_${item.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isBought) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isBought) 0.dp else 1.dp),
        border = if (item.isBought) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check Circle Bought Checkbox
            IconButton(
                onClick = onToggleBought,
                modifier = Modifier.testTag("chk_buy_${item.name}")
            ) {
                Icon(
                    imageVector = if (item.isBought) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                    contentDescription = "Estado comprado",
                    tint = if (item.isBought) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isBought) Color.Gray else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isBought) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${tagCfg.emoji} ${item.tag}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeFg
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val lineTotal = item.quantity * item.price
                Text(
                    text = "${item.quantity} ud x " + String.format(Locale.US, "%.2f €", item.price) + " = " + String.format(Locale.US, "%.2f €", lineTotal),
                    fontSize = 13.sp,
                    color = if (item.isBought) Color.Gray else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Price editor
            IconButton(
                onClick = onEditPrice,
                modifier = Modifier.testTag("edit_price_of_${item.name}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Cambiar precio",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}


// ==========================================
// SCREEN: HISTORY PURCHASE RECEIPT DETAILS
// ==========================================
@Composable
fun HistoryDetailsScreen(
    viewModel: ShoppingViewModel,
    groupId: Int,
    listName: String,
    timestamp: Long,
    total: Double
) {
    val items by viewModel.currentHistoryItems.collectAsState()
    val formattedDate = remember(timestamp) {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm", Locale("es", "ES"))
        sdf.format(Date(timestamp))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(extraLargePaddingHeader()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Detalle de Compra",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Compra realizada en $listName",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "CONCEPTO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(text = "Supermercado: $listName", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "FECHA Y HORA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(text = formattedDate, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "TOTAL DEL RECIBO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(
                    text = String.format(Locale.US, "%.2f €", total),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PRODUCTOS ADQUIRIDOS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val customTags = LocalCustomTags.current
                        val tc = getTagConfig(item.tag, customTags)
                        Text(text = tc.emoji, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "Pasillo: ${item.tag}", fontSize = 11.sp, color = Color.Gray)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val rowTotal = item.quantity * item.price
                            Text(
                                text = String.format(Locale.US, "%.2f €", rowTotal),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${item.quantity} ud x " + String.format(Locale.US, "%.2f €", item.price),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// AUXILIARY DIALOGS SECTION
// ==========================================

@Composable
fun CreateListDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva lista de la compra") },
        text = {
            Column {
                Text("Introduce el nombre de la lista (ej. Mercadona, Carrefour, Farmacia):", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("Ej. Mercadona") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_list_name")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onCreate(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("dialog_btn_create")
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun RenameListDialog(currentName: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar lista") },
        text = {
            Column {
                Text("Escribe el nuevo nombre de la lista:", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onRename(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    title: String,
    initialName: String = "",
    initialQuantity: Int = 1,
    initialTag: String = "Alimentación",
    initialPrice: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: Int, tag: String, price: Double) -> Unit,
    onDeleteTag: (CustomTag) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var quantity by remember { mutableStateOf(initialQuantity) }
    var priceStr by remember { mutableStateOf(if (initialPrice > 0.0) initialPrice.toString() else "") }
    var selectedTag by remember { mutableStateOf(initialTag) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del artículo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_prod_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Quantity adjust
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cantidad típica", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            IconButton(onClick = { if (quantity > 1) quantity-- }) {
                                Text("-", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(
                                text = quantity.toString(),
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { quantity++ }) {
                                Text("+", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    // Price input
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Precio (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_prod_price")
                    )
                }

                // Tag/Category selection (Mutable typed tag with suggestion chips!)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Pasillo / Categoría (Organización)", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedTag,
                        onValueChange = { selectedTag = it },
                        placeholder = { Text("Ej. Alimentación, Pasillo 3, Droguería...") },
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.List, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_prod_tag"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Categorías de tu listado (toca para seleccionar, 'x' para borrar):", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))

                    val customTags = LocalCustomTags.current
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        customTags.forEach { tag ->
                            val tc = getTagConfig(tag.name, customTags)
                            val isSelected = selectedTag.trim().equals(tag.name.trim(), ignoreCase = true)
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedTag = tag.name },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else tc.colorLight,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = tc.emoji, fontSize = 13.sp)
                                    Text(
                                        text = tag.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else tc.colorDark
                                    )
                                    // Soft defense: do not allow deleting the essential catch-all tag "Otros"
                                    if (!tag.name.equals("Otros", ignoreCase = true)) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Eliminar de la lista",
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else tc.colorDark.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    onDeleteTag(tag)
                                                    if (selectedTag.trim().equals(tag.name.trim(), ignoreCase = true)) {
                                                        selectedTag = "Otros"
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                onSave(name.trim(), quantity, selectedTag, finalPrice)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("dialog_prod_btn_save")
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
fun PriceUpdateDialog(
    itemName: String,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var priceStr by remember { mutableStateOf(if (currentPrice > 0.0) currentPrice.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actualizar Precio") },
        text = {
            Column {
                Text("Precio actual para '$itemName': " + String.format(Locale.US, "%.2f €", currentPrice), fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Nuevo precio real (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_update_price_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPrice = priceStr.toDoubleOrNull() ?: 0.0
                    onSave(finalPrice)
                },
                modifier = Modifier.testTag("dialog_update_price_btn")
            ) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Helper styling/color extension
fun Color.darken(factor: Float = 0.6f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}

// Extra responsive padding functions
@Composable
fun extraLargePaddingHeader(): PaddingValues {
    return PaddingValues(top = 24.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
}

@Composable
fun AuthorDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BackupRestoreDialog(
    onDismissRequest: () -> Unit,
    viewModel: ShoppingViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val contentResolver = context.contentResolver
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Import
    var exportFormat by remember { mutableStateOf(0) } // 0 = JSON, 1 = CSV
    var importFormat by remember { mutableStateOf(0) } // 0 = JSON, 1 = CSV
    var mergeData by remember { mutableStateOf(true) } // true = Merge, false = Overwrite

    var operationMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(true) }

    fun writeTextToUri(uri: android.net.Uri, text: String): Boolean {
        return try {
            contentResolver.openOutputStream(uri)?.use { os ->
                os.write(text.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readTextFromUri(uri: android.net.Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportAllDataToJson { text ->
                val success = writeTextToUri(uri, text)
                isSuccessMessage = success
                operationMessage = if (success) "¡Copia de seguridad (.json) exportada con éxito!" else "Error al escribir el archivo."
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportAllDataToCsv { text ->
                val success = writeTextToUri(uri, text)
                isSuccessMessage = success
                operationMessage = if (success) "¡Listas en formato CSV (.csv) exportadas con éxito!" else "Error al escribir el archivo."
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = readTextFromUri(uri)
            if (text != null) {
                val trimmedText = text.trim().let { if (it.startsWith("\uFEFF")) it.substring(1) else it }.trim()
                if (trimmedText.isEmpty()) {
                    isSuccessMessage = false
                    operationMessage = "El archivo seleccionado está vacío."
                } else {
                    val isJsonDetected = trimmedText.startsWith("{") || trimmedText.startsWith("[")
                    if (isJsonDetected) {
                        viewModel.importAllDataFromJson(trimmedText, overwrite = !mergeData) { ok ->
                            isSuccessMessage = ok
                            operationMessage = if (ok) "¡Archivo JSON importado con éxito!" else "Error al importar copia de seguridad JSON."
                        }
                    } else {
                        viewModel.importAllDataFromCsv(trimmedText, overwrite = !mergeData) { ok ->
                            isSuccessMessage = ok
                            operationMessage = if (ok) "¡Archivo CSV importado con éxito!" else "Error al importar listas CSV. Compruebe la cabecera / líneas."
                        }
                    }
                }
            } else {
                isSuccessMessage = false
                operationMessage = "No se pudo leer el archivo seleccionado o está vacío."
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header Title with Eco Accent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().testTag("backup_dialog_title"),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Copia de Seguridad",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tabs for EXPORT vs IMPORT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedSubTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedSubTab = 0; operationMessage = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Exportar",
                            color = if (selectedSubTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedSubTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedSubTab = 1; operationMessage = null }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Importar",
                            color = if (selectedSubTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error or success messages
                operationMessage?.let { msg ->
                    Surface(
                        color = if (isSuccessMessage) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSuccessMessage) Icons.Default.Check else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isSuccessMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSuccessMessage) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (selectedSubTab == 0) {
                    // --- EXPORT SUB-TAB ---
                    Text(
                        "Selecciona el formato de exportación:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Format Radio selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (exportFormat == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { exportFormat = 0; operationMessage = null }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = exportFormat == 0,
                                onClick = { exportFormat = 0; operationMessage = null },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Column {
                                Text("JSON", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Copia total", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (exportFormat == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { exportFormat = 1; operationMessage = null }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = exportFormat == 1,
                                onClick = { exportFormat = 1; operationMessage = null },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Column {
                                Text("CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Excel", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (exportFormat == 0) {
                            "El formato JSON exporta todas tus listas de la compra con sus productos, el historial de compras terminado y todas las etiquetas personalizadas creadas."
                        } else {
                            "El formato CSV exporta las listas de la compra actuales con sus artículos en formato separado por comas, compatible para abrir en hojas de cálculo."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Button for Exporting
                    Button(
                        onClick = {
                            if (exportFormat == 0) {
                                createJsonLauncher.launch("copia_seguridad_carrito.json")
                            } else {
                                createCsvLauncher.launch("listas_compra.csv")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_export_to_file"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (exportFormat == 0) "Crear y Guardar Archivo JSON" else "Crear y Guardar Archivo CSV",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                } else {
                    // --- IMPORT SUB-TAB ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Selecciona formato de importación:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Import choice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (importFormat == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { importFormat = 0; operationMessage = null }
                                    .padding(8.dp)
                            ) {
                                RadioButton(
                                    selected = importFormat == 0,
                                    onClick = { importFormat = 0; operationMessage = null },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text("JSON Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = if (importFormat == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { importFormat = 1; operationMessage = null }
                                    .padding(8.dp)
                            ) {
                                RadioButton(
                                    selected = importFormat == 1,
                                    onClick = { importFormat = 1; operationMessage = null },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text("CSV Listas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Conflict Strategy Option Choice
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { mergeData = !mergeData }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = mergeData,
                                onCheckedChange = { mergeData = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("checkbox_merge_settings")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    "Fusionar con datos existentes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Si lo desactivas, se borrarán todos tus datos anteriores",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Button for Importing
                        Button(
                            onClick = {
                                if (importFormat == 0) {
                                    openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                } else {
                                    openDocumentLauncher.launch(arrayOf("text/csv", "text/plain", "*/*"))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_select_file_to_import"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (importFormat == 0) "Seleccionar Archivo JSON" else "Seleccionar Archivo CSV",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_close_backup_dialog")
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
