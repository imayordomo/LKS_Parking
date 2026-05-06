package com.lksnext.ParkingIMayordomo.ui.pages

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lksnext.ParkingIMayordomo.R
import com.lksnext.ParkingIMayordomo.ui.components.ParkingBottomBar
import com.lksnext.ParkingIMayordomo.ui.components.ParkingDrawerContent
import com.lksnext.ParkingIMayordomo.ui.components.ParkingTopAppBar
import com.lksnext.ParkingIMayordomo.ui.theme.*
import com.lksnext.ParkingIMayordomo.ui.viewmodel.HelpViewModel
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_DASHBOARD
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HELP
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_HISTORY
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_NOTIFICATIONS
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_PROFILE
import com.lksnext.ParkingIMayordomo.utils.ParkingUtils.ROUTE_VIEW_PARKING
import kotlinx.coroutines.launch

private data class FAQ(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Help(
    viewModel: HelpViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val faqs = listOf(
        FAQ(stringResource(R.string.faq_q1), stringResource(R.string.faq_a1)),
        FAQ(stringResource(R.string.faq_q2), stringResource(R.string.faq_a2)),
        FAQ(stringResource(R.string.faq_q3), stringResource(R.string.faq_a3)),
        FAQ(stringResource(R.string.faq_q4), stringResource(R.string.faq_a4)),
        FAQ(stringResource(R.string.faq_q5), stringResource(R.string.faq_a5)),
        FAQ(stringResource(R.string.faq_q6), stringResource(R.string.faq_a6)),
        FAQ(stringResource(R.string.faq_q7), stringResource(R.string.faq_a7)),
        FAQ(stringResource(R.string.faq_q8), stringResource(R.string.faq_a8)),
        FAQ(stringResource(R.string.faq_q9), stringResource(R.string.faq_a9)),
        FAQ(stringResource(R.string.faq_q10), stringResource(R.string.faq_a10))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ParkingDrawerContent(
                currentRoute = ROUTE_HELP,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                ParkingTopAppBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationsClick = { onNavigate(ROUTE_NOTIFICATIONS) }
                )
            },
            bottomBar = {
                ParkingBottomBar(
                    selectedItem = -1,
                    onItemSelected = { index ->
                        val routes = listOf(ROUTE_DASHBOARD, ROUTE_HISTORY, ROUTE_PROFILE, ROUTE_VIEW_PARKING)
                        onNavigate(routes[index])
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.help_title),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = stringResource(R.string.help_subtitle),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                items(faqs) { faq ->
                    FAQAccordion(faq)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = stringResource(R.string.more_help_title),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.more_help_desc),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FAQAccordion(faq: FAQ) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = faq.question,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = faq.answer,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
