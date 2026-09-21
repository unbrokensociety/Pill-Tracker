package com.aistudio.meditracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = context.resources.configuration.locales[0].language

    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCircleIcon(
                        size = 40.dp,
                        tintColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (lang) {
                            "uk" -> "Політика конфіденційності"
                            "ru" -> "Политика конфиденциальности"
                            else -> "Privacy Policy"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = when (lang) {
                                "uk" -> "Закрити"
                                "ru" -> "Закрыть"
                                else -> "Close"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (lang) {
                                "uk" -> "Коротко: ваші ліки та історія прийому живуть лише на цьому пристрої. Мережа використовується винятково для перевірки оновлень із GitHub — медичні дані не надсилаються нікуди. Оновлення політики: вересень 2026."
                                "ru" -> "Коротко: ваши лекарства и история приёма живут только на этом устройстве. Сеть используется исключительно для проверки обновлений с GitHub — медицинские данные не отправляются никуда. Обновление политики: сентябрь 2026."
                                else -> "In short: your medications and intake history live only on this device. The network is used solely to check for updates on GitHub — no medical data ever leaves your phone. Policy updated: September 2026."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (lang) {
                        "uk" -> UkrainianPolicyContent()
                        "ru" -> RussianPolicyContent()
                        else -> EnglishPolicyContent()
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = when (lang) {
                                "uk" -> "Зрозуміло та приймаю"
                                "ru" -> "Понятно и принимаю"
                                else -> "I understand & accept"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UkrainianPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Локальне збереження даних",
        body = "Усі ваші дані — назви препаратів, розклади, часи прийому, позначки «прийнято» та залишки в упаковці — зберігаються виключно на вашому пристрої в локальній базі. Реєстрація, обліковий запис або вхід не потрібні: застосунок повністю працює без них."
    )
    PolicyCardSection(
        number = "2",
        title = "Мережева активність — лише оновлення",
        body = "Єдиний вихід у мережу — перевірка й завантаження нових версій застосунку з GitHub (api.github.com / github.com) за вашою командою або не частіше ніж раз на 3 години автоматично. Назви ліків, розклади та будь-які медичні дані по мережі не передаються взагалі."
    )
    PolicyCardSection(
        number = "3",
        title = "Дозволи Android та їхнє призначення",
        body = "Сповіщення — щоб нагадувати про прийом; точний будильник — щоб нагадування приходили вчасно, а не «колось»; інтернет — лише для оновлень (див. п. 2); дозвіл на встановлення — щоб оновлюватись прямо із застосунку (дається один раз). Жодних дозволів на контакти, камеру, геолокацію чи мікрофон немає й не буде."
    )
    PolicyCardSection(
        number = "4",
        title = "Резервні копії та хмара",
        body = "Автоматичне резервне копіювання Android (у ваш Google-акаунт) для цього застосунку вимкнено (allowBackup=false): навіть системними засобами дані не «з’їжджають» у хмару. Ваше ім’я для привітання теж зберігається лише локально й видаляється однією кнопкою."
    )
    PolicyCardSection(
        number = "5",
        title = "Ваші права та контроль",
        body = "Відповідно до Закону України № 2297-VI «Про захист персональних даних» та GDPR (ЄС 2016/679) ви маєте право на доступ, виправлення й повне видалення своїх даних. Видалення застосунку або «Очистити дані» в системних налаштуваннях знищує всю базу безповоротно й одразу."
    )
    PolicyCardSection(
        number = "6",
        title = "Без аналітики й реклами",
        body = "Ми не збираємо статистику використання, не підключаємо аналітику, не показуємо рекламу й не передаємо дані третім особам — немає кому передавати: серверів у нас для ваших даних просто не існує."
    )
    PolicyCardSection(
        number = "7",
        title = "Медичне застереження",
        body = "Pill Tracker — особистий органайзер і нагадування, а не медичний пристрій. Він не ставить діагнозів, не призначає лікування й не замінює консультацію лікаря. Дозування та розклад завжди узгоджуйте з фахівцем."
    )
    PolicyCardSection(
        number = "8",
        title = "Зміни політики",
        body = "Оновлена редакція набирає чинності з моменту публікації в застосунку. Істотні зміни супроводжуватимуться описом «що нового» у відповідному оновленні."
    )
}

@Composable
private fun RussianPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Локальное хранение данных",
        body = "Все ваши данные — названия препаратов, расписания, отметки «принято» и остатки в упаковке — хранятся исключительно на вашем устройстве в локальной базе. Регистрация и аккаунт не нужны: приложение полностью работает без них."
    )
    PolicyCardSection(
        number = "2",
        title = "Сетевая активность — только обновления",
        body = "Единственное обращение в сеть — проверка и загрузка новых версий приложения с GitHub (api.github.com / github.com) по вашей команде или автоматически не чаще раза в 3 часа. Названия лекарств, расписания и любые медицинские данные по сети не передаются вообще."
    )
    PolicyCardSection(
        number = "3",
        title = "Разрешения Android и их назначение",
        body = "Уведомления — напоминать о приёме; точный будильник — чтобы напоминания приходили вовремя; интернет — только для обновлений (см. п. 2); разрешение на установку — чтобы обновляться прямо из приложения (даётся один раз). Разрешений на контакты, камеру, геолокацию или микрофон нет и не будет."
    )
    PolicyCardSection(
        number = "4",
        title = "Резервные копии и облако",
        body = "Автоматическое резервное копирование Android (в ваш Google-аккаунт) для этого приложения отключено (allowBackup=false): даже системными средствами данные не уезжают в облако. Ваше имя для приветствия тоже хранится только локально и удаляется одной кнопкой."
    )
    PolicyCardSection(
        number = "5",
        title = "Ваши права и контроль",
        body = "Согласно Закону Украины № 2297-VI «О защите персональных данных» и GDPR (ЕС 2016/679) вы вправе получить доступ, исправить или полностью удалить свои данные. Удаление приложения или «Очистить данные» в системных настройках уничтожает всю базу безвозвратно и сразу."
    )
    PolicyCardSection(
        number = "6",
        title = "Без аналитики и рекламы",
        body = "Мы не собираем статистику использования, не подключаем аналитику, не показываем рекламу и не передаём данные третьим лицам — некому передавать: серверов для ваших данных у нас просто не существует."
    )
    PolicyCardSection(
        number = "7",
        title = "Медицинская оговорка",
        body = "Pill Tracker — личный органайзер и напоминание, а не медицинский прибор. Он не ставит диагнозов и не заменяет консультацию врача. Дозировку и график всегда согласовывайте со специалистом."
    )
    PolicyCardSection(
        number = "8",
        title = "Изменения политики",
        body = "Обновлённая редакция вступает в силу с момента публикации в приложении. Существенные изменения будут описаны в «что нового» соответствующего обновления."
    )
}

@Composable
private fun EnglishPolicyContent() {
    PolicyCardSection(
        number = "1",
        title = "Local data storage",
        body = "All your data — medication names, schedules, intake marks and package stock — is stored exclusively on your device in a local database. No registration or account is required: the app works fully without them."
    )
    PolicyCardSection(
        number = "2",
        title = "Network activity — updates only",
        body = "The only network access is checking for and downloading new app versions from GitHub (api.github.com / github.com), either on your request or automatically at most once every 3 hours. Medication names, schedules and any medical data are never transmitted over the network."
    )
    PolicyCardSection(
        number = "3",
        title = "Android permissions and why",
        body = "Notifications — to remind you about intakes; exact alarms — so reminders fire on time; Internet — only for updates (see §2); install permission — to update straight from the app (granted once). No permissions for contacts, camera, location or microphone — now or ever."
    )
    PolicyCardSection(
        number = "4",
        title = "Backups and the cloud",
        body = "Android automatic backup (to your Google account) is disabled for this app (allowBackup=false): your data never reaches any cloud, even via system mechanisms. Your greeting name is also stored locally and can be removed with one tap."
    )
    PolicyCardSection(
        number = "5",
        title = "Your rights and control",
        body = "Under the Law of Ukraine № 2297-VI and the EU GDPR (2016/679) you may access, rectify and fully erase your data. Uninstalling the app or using “Clear data” in system settings destroys the whole database immediately and irreversibly."
    )
    PolicyCardSection(
        number = "6",
        title = "No analytics, no ads",
        body = "We collect no usage statistics, embed no analytics, show no ads and share nothing with third parties — there is simply no server of ours that your data could reach."
    )
    PolicyCardSection(
        number = "7",
        title = "Medical disclaimer",
        body = "Pill Tracker is a personal organizer and reminder, not a medical device. It does not diagnose, prescribe or replace consulting a physician. Always confirm dosage and schedule with a professional."
    )
    PolicyCardSection(
        number = "8",
        title = "Changes to this policy",
        body = "An updated revision takes effect once published inside the app. Significant changes will be described in the “what’s new” notes of the corresponding update."
    )
}

@Composable
private fun PolicyCardSection(
    number: String,
    title: String,
    body: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}
