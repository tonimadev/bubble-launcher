package digital.tonima.bubbleslauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import digital.tonima.bubbleslauncher.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OnboardingPage(
    val titleRes: Int,
    val descRes: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubblesViewModel: BubblesViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(R.string.onboarding_title_1, R.string.onboarding_desc_1),
        OnboardingPage(R.string.onboarding_title_2, R.string.onboarding_desc_2),
        OnboardingPage(R.string.onboarding_title_3, R.string.onboarding_desc_3),
        OnboardingPage(R.string.onboarding_title_4, R.string.onboarding_desc_4)
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    // Spawn some bubbles initially to show how the feature looks
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) {
            val colors = listOf(
                Color(0xFFFF5252), // Red
                Color(0xFF448AFF), // Blue
                Color(0xFF69F0AE), // Green
                Color(0xFFFFD740), // Yellow
                Color(0xFFE040FB)  // Purple
            )
            for (i in 0..6) {
                delay(300)
                bubblesViewModel.onImpulseResisted(colors[i % colors.size])
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)) // Deep dark background for gaming theme
    ) {
        // Render the beautiful aquarium background
        MentalAquariumBackground(viewModel = bubblesViewModel)

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = pages[page].titleRes),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 38.sp,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(id = pages[page].descRes),
                            color = Color(0xFFE0E0E0),
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color(0xFF00E676) else Color.DarkGray
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                        )
                    }
                }

                if (pagerState.currentPage == pages.size - 1) {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676), // Vibrant gaming neon green
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.onboarding_action).uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.onboarding_btn_next).uppercase(),
                            color = Color(0xFF00E676),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
