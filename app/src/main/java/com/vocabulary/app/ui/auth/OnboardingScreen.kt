package com.vocabulary.app.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vocabulary.app.ui.components.AppIcons
import com.vocabulary.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class IntroPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val chipIcon: ImageVector,
    val chip: String
)

private val pages = listOf(
    IntroPage(
        icon = AppIcons.Cards,
        title = "Words that stay with you",
        body = "Flip through cards with Bangla meanings, synonyms, antonyms and real examples.",
        chipIcon = AppIcons.Volume,
        chip = "abandon  \u00B7  \u09A4\u09CD\u09AF\u09BE\u0997 \u0995\u09B0\u09BE"
    ),
    IntroPage(
        icon = AppIcons.Flame,
        title = "Build a daily habit",
        body = "A word of the day, a daily goal and a streak that keeps you coming back.",
        chipIcon = AppIcons.Flame,
        chip = "7 day streak"
    ),
    IntroPage(
        icon = AppIcons.Pencil,
        title = "Test what you know",
        body = "Quick quizzes on meanings, synonyms, idioms and more. Mistakes are saved so you can fix them.",
        chipIcon = AppIcons.CheckCircle,
        chip = "Score 9 / 10"
    ),
    IntroPage(
        icon = AppIcons.Download,
        title = "Learn anywhere, offline",
        body = "Your word list is saved on your phone and updates itself when you are online.",
        chipIcon = AppIcons.Check,
        chip = "Ready offline"
    )
)

/** Four intro slides shown once, before the sign-in screen. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex
    val scheme = MaterialTheme.colorScheme

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(scheme.primary.copy(alpha = 0.14f), scheme.background, scheme.background)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Content, vertical = Spacing.Compact),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinish, modifier = Modifier.alpha(if (isLast) 0f else 1f), enabled = !isLast) {
                    Text("Skip")
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                val offset = ((pagerState.currentPage - index) + pagerState.currentPageOffsetFraction)
                    .absoluteValue.coerceIn(0f, 1f)
                IntroPageContent(
                    page = pages[index],
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f - offset * 0.85f
                        val s = 1f - offset * 0.12f
                        scaleX = s
                        scaleY = s
                        translationX = offset * 40f * (if (pagerState.currentPage > index) -1 else 1)
                    }
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Section, vertical = Spacing.Section),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Section)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(pages.size) { i ->
                        val active = i == pagerState.currentPage
                        val width by animateDpAsState(if (active) 26.dp else 8.dp, tween(260), label = "dotWidth")
                        val color by animateColorAsState(
                            if (active) scheme.primary else scheme.outlineVariant, tween(260), label = "dotColor"
                        )
                        Box(Modifier.height(8.dp).width(width).clip(CircleShape).background(color))
                    }
                }
                Button(
                    onClick = {
                        if (isLast) onFinish()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                ) {
                    Text(if (isLast) "Get started" else "Next", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun IntroPageContent(page: IntroPage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HeroIllustration(icon = page.icon, chipIcon = page.chipIcon, chip = page.chip)
        Spacer(Modifier.height(Spacing.Major))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.Standard))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Soft pulsing rings around a solid icon disc, with a small floating sample chip under it. */
@Composable
private fun HeroIllustration(icon: ImageVector, chipIcon: ImageVector, chip: String) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "hero")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "pulse"
    )
    val floatY by transition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "float"
    )

    Box(modifier = Modifier.size(width = 260.dp, height = 250.dp), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier.size(210.dp).scale(pulse).clip(CircleShape).background(scheme.primary.copy(alpha = 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(150.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(44.dp))
                }
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .graphicsLayer { translationY = floatY.dp.toPx() }
                .border(1.dp, scheme.outlineVariant, CircleShape),
            shape = CircleShape,
            color = scheme.surfaceContainer,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(chipIcon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                Text(chip, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
