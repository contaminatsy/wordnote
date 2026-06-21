package com.example.wordnote2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import android.widget.Toast
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.wordnote2.ui.theme.Wordnote2Theme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Wordnote2Theme {
                Wordnote2App()
            }
        }
    }
}

@Composable
fun Wordnote2App() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.SEARCH) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        // 采用最直接的文字 Emoji 代替图标，彻底避免找图片或导包失败
                        val emoji = when(it) {
                            AppDestinations.SEARCH -> "🔍"
                            AppDestinations.REVIEW -> "📚"
                            AppDestinations.TEST -> "✏️"
                        }
                        Text(emoji, fontSize = 20.sp)
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            // 根据底部导航的切换，展示对应的页面
            when (currentDestination) {
                AppDestinations.SEARCH -> SearchScreen(modifier)
                AppDestinations.REVIEW -> ReviewScreen(modifier)
                AppDestinations.TEST -> TestScreen(modifier)
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    SEARCH("查询", R.drawable.ic_home),          // 暂用默认图标，后面可以换
    REVIEW("复习", R.drawable.ic_favorite),
    TEST("测试", R.drawable.ic_account_box),
}



@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // 新增：加载中状态

    var wordTitle by remember { mutableStateOf("") }
    var wordPhonetic by remember { mutableStateOf("") }
    var wordMeanings by remember { mutableStateOf<List<Meaning>>(emptyList()) }
    var errorMessage by remember {mutableStateOf("")}

    // 获取协程作用域，用来在点击按钮时开启异步线程
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔍 单词查询",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // 搜索栏区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("输入要查询的英文单词") },
                placeholder = { Text("例如: run") },
                leadingIcon = { Text("🔍", modifier = Modifier.padding(start = 8.dp)) },
                modifier = Modifier.weight(1.0f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = { /* 拍照逻辑 */ },
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("📷", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 查询按钮
        Button(
            onClick = {
                if (searchQuery.isNotBlank()) {
                    // 开启协程在后台线程请求网络
                    coroutineScope.launch {
                        isLoading = true
                        showResult = false
                        try {
                            // 🚀 真正发起网络请求
                            val responseList = NetworkClient.apiService.getWordInfo(searchQuery.trim().lowercase())
                            if (responseList.isNotEmpty()) {
                                val result = responseList[0]
                                wordTitle = result.word
                                wordPhonetic = result.phonetic ?: "[暂无音标]"

                                wordMeanings = result.meanings ?: emptyList()
                                showResult = true
                            }
                        } catch (e: Exception) {
                            // 请求失败（比如单词拼错、没网）
                            wordTitle = searchQuery
                            wordPhonetic = ""
                            wordMeanings = emptyList()
                            errorMessage = "未查询到该单词或网络异常"
                            showResult = true
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading // 加载中时禁用按钮防止重复点击
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("立即查询", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 查询结果卡片
        AnimatedVisibility(visible = showResult) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .heightIn(max = 450.dp) // 限制最大高度，防止把底部的导航栏挤出屏幕
                        .verticalScroll(rememberScrollState()) // 开启滚动
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = wordTitle, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = wordPhonetic, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    if(errorMessage.isNotBlank()){
                        Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                    }

                    wordMeanings.forEach { meaning ->
                        // 1. 显示词性标签 (例如: noun, verb)
                        Text(
                            text = "【${meaning.partOfSpeech}】",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )

                        // 2. 遍历该词性下的所有释义定义
                        meaning.definitions.forEachIndexed { index, def ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, bottom = 8.dp)
                            ) {
                                // 释义文本（带上序号，比如 1. 2. 3.）
                                Text(
                                    text = "${index + 1}. ${def.definition}",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // 如果有对应的例句，就展示例句
                                if (!def.example.isNullOrBlank()) {
                                    Text(
                                        text = "例句: ${def.example}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            // 开启后台协程，防止数据库写入操作卡住界面
                            coroutineScope.launch {
                                // 1. 获取数据库单例
                                val db = AppDatabase.getDatabase(context)

                                // 2. 数据降维：把复杂的 JSON 多词性列表，拼接成一段紧凑的字符串方便存储
                                val compactTranslation = wordMeanings.joinToString("\n") { meaning ->
                                    "[${meaning.partOfSpeech}] " + (meaning.definitions.firstOrNull()?.definition ?: "")
                                }

                                // 3. 构造一行表数据
                                val newWord = WordEntity(
                                    word = wordTitle,
                                    phonetic = wordPhonetic,
                                    translation = compactTranslation
                                )

                                // 4. 执行写入
                                db.wordDao().insertWord(newWord)

                                // 5. 在主线程弹出成功提示
                                Toast.makeText(context, "✅ 成功加入单词本！", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("➕ 加入我的单词本")
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // 1. 获取数据库实例
    val db = remember { AppDatabase.getDatabase(context) }
    // 2. 🚀 核心魔法：将 Room 的 Flow 数据流转换为 Compose 的 State
    // 只要数据库有任何增删改，wordList 就会自动刷新，不需要手动去查！
    val allWords by db.wordDao().getAllWords().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("未掌握", "已掌握")

    val displayList = allWords.filter { word ->
        if (selectedTabIndex == 0) !word.isMastered else word.isMastered
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📚 我的单词本",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // 3. 判断数据库里有没有数据
        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedTabIndex == 0) "太棒了，所有单词都掌握了！" else "还没有已掌握的单词哦~",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayList) { wordEntity ->
                    WordItemCard(
                        word = wordEntity,
                        onDeleteClick = {
                            coroutineScope.launch { db.wordDao().deleteWord(wordEntity) }
                        },
                        onToggleMastered = {
                            // 🚀 核心逻辑：使用 copy 复制一份当前数据并反转 isMastered，然后更新进数据库
                            coroutineScope.launch {
                                val updatedWord = wordEntity.copy(isMastered = !wordEntity.isMastered)
                                db.wordDao().updateWord(updatedWord)
                            }
                        }
                    )
                }
            }
        }
    }
}

// 这是列表里每一个小卡片的 UI 组件
@Composable
fun WordItemCard(
    word: WordEntity,
    onDeleteClick: () -> Unit,
    onToggleMastered: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            // 如果已掌握，给卡片一个淡淡的绿色背景；未掌握则是默认颜色
            containerColor = if (word.isMastered)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = word.word, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = word.phonetic, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                }

                // 操作按钮区
                Row {
                    // 状态切换按钮 (未掌握显示✅，已掌握显示⏪)
                    IconButton(onClick = onToggleMastered) {
                        Text(if (word.isMastered) "⏪" else "✅", fontSize = 20.sp)
                    }
                    // 删除按钮
                    IconButton(onClick = onDeleteClick) {
                        Text("🗑️", fontSize = 20.sp)
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = word.translation,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TestScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    // 订阅数据源
    val allWords by db.wordDao().getAllWords().collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()

    // UI 交互状态调度
    var currentWord by remember { mutableStateOf<WordEntity?>(null) }
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedOption by remember { mutableStateOf<String?>(null) }

    // 核心管线：生成下一道题目
    fun generateNextQuestion() {
        if (allWords.size >= 4) {
            val target = allWords.filter { it != currentWord }.random()
            // 提取简短释义（取第一行）作为正确选项

            val correctOption = target.translation.split("\n").firstOrNull() ?: target.translation

            // 从词库里随机抓 3 个不一样的词作为干扰项
            val wrongWords = allWords.filter { it.word != target.word }.shuffled().take(3)
            val wrongOptions = wrongWords.map { it.translation.split("\n").firstOrNull() ?: it.translation }

            // 装填弹药并打乱顺序
            currentWord = target
            options = (wrongOptions + correctOption).shuffled()
            selectedOption = null // 重置玩家选择
        }
    }

    // 将题目中的单词标记为彻底掌握
    fun masterWord(word: WordEntity) {
        coroutineScope.launch {
            // 1. 统一参数名为 word，2. 单向锁定状态为 true
            val updatedWord = word.copy(isMastered = true)
            db.wordDao().updateWord(updatedWord)

            // 3. 顺手做一个体验优化：点完“已掌握”后，直接自动跳转到下一题！
            generateNextQuestion()
        }
    }



    // 监听数据池加载，只要词库满 4 个词，立刻初始化第一题
    LaunchedEffect(allWords) {
        if (allWords.size >= 4 && currentWord == null) {
            generateNextQuestion()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✏️ 趣味测试", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

        // 拦截条件：词库太少玩不了
        if (allWords.size < 4) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "你的词库还不够丰富（至少需要 4 个单词），\n快去查询页面多加几个词来解锁测试吧！",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            // 游戏主场景
            currentWord?.let { targetWord ->
                // 题目展示卡片
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp).padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = targetWord.word, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = targetWord.phonetic, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 答题选项网格 (4选1)
                val correctOption = targetWord.translation.split("\n").firstOrNull() ?: targetWord.translation

                options.forEach { optionText ->
                    val isSelected = selectedOption == optionText
                    val isCorrect = optionText == correctOption

                    // 动态结算颜色：选对标绿，选错标红，没点时保持默认色
                    val buttonColor = if (selectedOption != null) {
                        if (isCorrect) Color(0xFF4CAF50)
                        else if (isSelected) Color(0xFFE53935)
                        else MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    Button(
                        onClick = {
                            // 锁死机制：只有没选过的情况下才允许点击，防止反复选
                            if (selectedOption == null) selectedOption = optionText
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = optionText,
                            fontSize = 15.sp,
                            color = if (selectedOption != null && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 下一题按钮 (只有答题结算后才会弹出)
                AnimatedVisibility(visible = selectedOption != null) {
                    Button(
                        onClick = { generateNextQuestion() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("下一题 ➔", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                //记住按钮
                TextButton(
                    onClick = {
                        currentWord?.let { masterWord(it) }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("✅ 太简单了，标记为已掌握", fontSize = 16.sp, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

// 1. Entity（实体类）：映射为 SQLite 数据库中的一张表
@Entity(tableName = "words_table")
data class WordEntity(
    @PrimaryKey
    val word: String, // 用单词本身作为主键，防止同一个单词被重复添加多次
    val phonetic: String,
    val translation: String, // 为了方便存储，我们会把复杂的释义拼接成一段文本存进这个字段
    val addedTime: Long = System.currentTimeMillis(), // 记录存入时间，以后复习页面可以按时间倒序排列
    val isMastered: Boolean = false // 👈 新增：默认刚加入的单词都是“未掌握”
)

// 2. DAO（数据访问对象）：定义数据库的增删改查操作
@Dao
interface WordDao {
    // 插入单词。如果遇到主键冲突（单词已存在），则替换更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity)

    // 查询所有单词。返回 Flow 代表这是一个“响应式数据流”，只要数据库有变化，界面会自动刷新
    @Query("SELECT * FROM words_table ORDER BY addedTime DESC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Update
    suspend fun updateWord(word: WordEntity)
}

// 3. Database（数据库引擎实例）：单例模式
@Database(entities = [WordEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 获取数据库实例的标准写法，保证多线程安全
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wordnote_database" // 这是保存在手机存储里的真实物理文件名
                )
                    .fallbackToDestructiveMigration(false) // 👈 新增：允许清空旧表重建新表，防止崩溃
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


data class DictionaryResponse(
    val word: String,
    val phonetic: String?,
    val meanings: List<Meaning>?
)

data class Meaning(
    val partOfSpeech: String,
    val definitions: List<Definition>
)

data class Definition(
    val definition: String,
    val example: String?
)

// 2. 定义请求的 API 接口
interface DictionaryApiService {
    // 动态把单词拼接到 URL 后面
    @GET("api/v2/entries/en/{word}")
    suspend fun getWordInfo(@Path("word") word: String): List<DictionaryResponse>
}

// 3. 创建单例网络请求客户端
object NetworkClient {
    private const val BASE_URL = "https://api.dictionaryapi.dev/"

    val apiService: DictionaryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // 自动将 JSON 转换为 Kotlin 对象
            .build()
            .create(DictionaryApiService::class.java)
    }
}