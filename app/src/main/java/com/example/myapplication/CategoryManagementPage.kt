package com.example.myapplication

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.data.Category
import com.example.myapplication.utils.displayName
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementPage(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var showRestoreSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_manage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.category_add))
            }
        },
        snackbarHost = {
            if (showRestoreSuccess) {
                Snackbar {
                    Text(stringResource(R.string.category_restore_success))
                }
            }
        }
    )
     { padding ->
        val defaultCategories = categories.filter { it.isDefault }
        val customCategories = categories.filter { !it.isDefault }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)  // 添加底部内边距，避免被浮动按钮遮挡
        ) {
            // 默认类别
            if (defaultCategories.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.category_default_section),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                    )
                }

                items(defaultCategories) { category ->
                    CategoryListItem(
                        category = category,
                        onDelete = {
                            categoryToDelete = category  // 显示确认对话框
                        }
                    )
                }
            }

            // 恢复默认类别区域
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.category_restore_default),
                                fontSize = 14.sp,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.category_restore_default_desc),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    viewModel.restoreDefaultCategories()
                                    showRestoreSuccess = true
                                    delay(2000)
                                    showRestoreSuccess = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            // 自定义类别
            item {
                Text(
                    text = stringResource(R.string.category_custom_section, customCategories.size),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
            }

            if (customCategories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.category_empty_custom),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.category_empty_custom_hint),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(customCategories) { category ->
                    CategoryListItem(
                        category = category,
                        onDelete = {
                            scope.launch {
                                viewModel.deleteCategory(category)
                            }
                        }
                    )
                }
            }
        }
    }

    // 添加类别对话框
    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, iconName, iconPath, color ->
                scope.launch {
                    viewModel.addCategory(
                        Category(
                            name = name,
                            iconName = iconName,
                            iconPath = iconPath,
                            color = color,
                            isDefault = false
                        )
                    )
                    showAddDialog = false
                }
            }
        )
    }
    // 删除类别确认对话框
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(stringResource(R.string.category_delete_confirm)) },
            text = {
                Text(stringResource(R.string.category_delete_message_with_name, categoryToDelete?.displayName().orEmpty()))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { category ->
                            scope.launch {
                                viewModel.deleteCategory(category)
                                categoryToDelete = null
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 显示图标（优先显示自定义图片）
                if (category.iconPath != null) {
                    AsyncImage(
                        model = category.iconPath,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        imageVector = getCategoryIcon(category.iconName),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = category.displayName(),
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (category.isDefault) {
                        Text(
                            text = stringResource(R.string.category_default_badge),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.category_custom_label),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 删除按钮（只有自定义类别才显示）
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconName: String?, iconPath: String?, color: String) -> Unit
) {
    val context = LocalContext.current
    var categoryName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("more_horiz") }
    var selectedColor by remember { mutableStateOf("#9C27B0") }
    var customImageUri by remember { mutableStateOf<Uri?>(null) }
    var customImagePath by remember { mutableStateOf<String?>(null) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // 将图片复制到应用私有目录
            try {
                val fileName = "category_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                customImagePath = file.absolutePath
                customImageUri = uri
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 可选图标列表
    val availableIcons = listOf(
        "restaurant" to "餐饮",
        "directions_car" to "交通",
        "shopping_cart" to "购物",
        "movie" to "娱乐",
        "local_hospital" to "医疗",
        "school" to "教育",
        "home" to "居住",
        "fitness_center" to "健身",
        "pets" to "宠物",
        "build" to "维修",
        "phone" to "通讯",
        "book" to "书籍",
        "flight" to "旅行",
        "celebration" to "庆祝",
        "face" to "美容",
        "sports_esports" to "游戏",
        "more_horiz" to "其他"
    )

    // 可选颜色
    val availableColors = listOf(
        "#FF5722" to "红色",
        "#E91E63" to "粉色",
        "#9C27B0" to "紫色",
        "#2196F3" to "蓝色",
        "#00BCD4" to "青色",
        "#4CAF50" to "绿色",
        "#FF9800" to "橙色",
        "#607D8B" to "灰色"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_add_custom_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // 类别名称
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text(stringResource(R.string.category_name_hint)) },
                    placeholder = { Text(stringResource(R.string.category_name_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 选择图标方式
                Text(
                    text = stringResource(R.string.category_select_icon),
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 自定义图片按钮
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (customImageUri != null) stringResource(R.string.category_custom_image_selected) else stringResource(R.string.category_select_image_from_gallery)
                    )
                }

                if (customImageUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // 显示预览
                    AsyncImage(
                        model = customImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    TextButton(
                        onClick = {
                            customImageUri = null
                            customImagePath = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.category_remove_image))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.category_or_choose_preset_icon),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 图标网格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableIcons.take(6).forEach { (icon, _) ->
                        IconButton(
                            onClick = {
                                selectedIcon = icon
                                customImageUri = null
                                customImagePath = null
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(icon),
                                contentDescription = null,
                                tint = if (selectedIcon == icon && customImageUri == null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 选择颜色
                Text(
                    text = stringResource(R.string.category_select_color),
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableColors.forEach { (color, _) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = MaterialTheme.shapes.small,
                                color = androidx.compose.ui.graphics.Color(
                                    android.graphics.Color.parseColor(color)
                                )
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (categoryName.isNotBlank()) {
                        onConfirm(
                            categoryName,
                            if (customImagePath != null) null else selectedIcon,
                            customImagePath,
                            selectedColor
                        )
                    }
                },
                enabled = categoryName.isNotBlank()
            ) {
                Text(stringResource(R.string.common_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}