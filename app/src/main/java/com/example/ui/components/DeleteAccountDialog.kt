package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun DeleteAccountDialog(
    pendingDeletionTimestamp: Long,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLang = context.resources.configuration.locales[0].language
    val isPending = pendingDeletionTimestamp > 0L

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isPending) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPending) Icons.Filled.Warning else Icons.Filled.DeleteForever,
                            contentDescription = "Delete Account",
                            tint = if (isPending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (currentLang) {
                        "uk" -> if (isPending) "Акаунт у процесі видалення" else "Видалення облікового запису"
                        "ru" -> if (isPending) "Аккаунт в процессе удаления" else "Удаление учётной записи"
                        else -> if (isPending) "Account Scheduled for Deletion" else "Delete Account & Personal Data"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when (currentLang) {
                        "uk" -> "Право на забуття (ст. 15 Закону України «Про захист персональних даних»)"
                        "ru" -> "Право на забвение (ст. 15 Закона Украины «О защите персональных данных»)"
                        else -> "Right to Erasure (Art. 15 Ukrainian Law № 2297-VI & GDPR)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isPending) {
                        Text(
                            text = when (currentLang) {
                                "uk" -> "Запит на видалення було прийнято. Ваш акаунт та дані будуть остаточно знищені через 30 днів.\n\nВи маєте повне право скасувати видалення у будь-який момент протягом цього 30-денного терміну."
                                "ru" -> "Запрос на удаление принят. Ваш аккаунт и данные будут окончательно уничтожены через 30 дней.\n\nВы имеете полное право отменить удаление в любой момент в течение этих 30 дней."
                                else -> "Your deletion request was accepted. Your account and personal health entries will be permanently purged in 30 days.\n\nYou can cancel this deletion at any time during this 30-day grace period."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = when (currentLang) {
                                "uk" -> "Натискаючи кнопку видалення, ви ініціюєте процедуру остаточного знищення вашого облікового запису та всіх збережених даних відповідно до законодавства.\n\n• Процес видалення має 30-денний відкладений період.\n• Протягом 30 днів ви можете скасувати видалення у додатку.\n• Після закінчення 30 днів всі розклади, прийоми та хмарні резервні копії будуть безповоротно видалені з серверів."
                                "ru" -> "Нажимая кнопку удаления, вы инициируете процедуру окончательного уничтожения учётной записи и всех данных.\n\n• Процедура включает 30-дневный отложенный период.\n• В течение 30 дней вы можете отменить удаление в приложении.\n• По истечении 30 дней все данные уничтожаются безвозвратно."
                                else -> "By initiating account deletion, your profile and all personal health logs will be permanently erased pursuant to data protection law.\n\n• A mandatory 30-day grace period is enforced.\n• You can cancel deletion anytime during these 30 days.\n• After 30 days, all cloud and local records are permanently wiped."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                if (isPending) {
                    Button(
                        onClick = {
                            onCancelDelete()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when (currentLang) {
                                "uk" -> "Скасувати видалення акаунту"
                                "ru" -> "Отменить удаление аккаунта"
                                else -> "Cancel Account Deletion"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            onRequestDelete()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when (currentLang) {
                                "uk" -> "Підтвердити видалення (30 днів)"
                                "ru" -> "Подтвердить удаление (30 дней)"
                                else -> "Confirm Deletion Request (30 Days)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when (currentLang) {
                                "uk" -> "Скасувати"
                                "ru" -> "Отмена"
                                else -> "Cancel"
                            }
                        )
                    }
                }
            }
        }
    }
}
