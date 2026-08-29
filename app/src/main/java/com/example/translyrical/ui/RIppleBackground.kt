package com.example.translyrical.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap

const val RIPPLE_SHADER_SRC = """
    uniform float2 uResolution;
    uniform float uTime;
    uniform shader uIcon;
    uniform float2 uIconNativeSize;
    
    float rippleIntensity(float2 p) {
        float2 uv = p / uResolution.xy;
        uv = uv * 2.0 - 1.0;
        float aspect = uResolution.x / uResolution.y;
        uv.x *= aspect;
        float d = length(uv);
        float ripple = sin(d * 40. - uTime * 5.);
        ripple = abs(ripple);
        float finalShape = smoothstep(0.0, 0.1, ripple);
        finalShape *= exp(-length(uv) * 2.5);
        return finalShape;
    }
    half4 main(float2 fragCoord) {
    float backgroundRipple = rippleIntensity(fragCoord);
    float3 backgroundColor = float3(0.0, 0.0, 1.0) * backgroundRipple;
    backgroundColor *= 0.7;

    float iconScale = uResolution.x * 0.18;
    float2 iconSize = float2(iconScale, iconScale);
    float2 centerOffset = (uResolution.xy - iconSize) * 0.5;
    
    float bobOffset = sin(uTime * 2.5) * 15.0; 
    float2 animatedIconPos = centerOffset + float2(0.0, bobOffset);
    
    half4 iconColor = half4(0.0);
    
    if (fragCoord.x > animatedIconPos.x && fragCoord.x < (animatedIconPos.x + iconSize.x) &&
        fragCoord.y > animatedIconPos.y && fragCoord.y < (animatedIconPos.y + iconSize.y)) {
        float2 iconUv = (fragCoord - animatedIconPos) / iconSize;
        float2 sampleCoord = iconUv * uIconNativeSize;
        iconColor = uIcon.eval(sampleCoord); 
        iconColor.rgb = float3(1.0);
    }

    float3 finalColor = (iconColor.rgb * iconColor.a) + (backgroundColor * (1.0 - iconColor.a));
    return half4(finalColor, 1.0);
    }
"""
@Composable
fun RippleBackground(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = 0,
    content: @Composable () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current

        val infiniteTransition = rememberInfiniteTransition(label = "time")
        val uTime by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 60f,
            animationSpec = InfiniteRepeatableSpec(
                animation = tween(60000, easing = LinearEasing)
            ),
            label = "timeUniform"
        )
        val runtimeShader = remember { RuntimeShader(RIPPLE_SHADER_SRC) }
        val shaderBrush = remember { ShaderBrush(runtimeShader) }

        remember(iconRes, windowInfo) {
            val targetSizePx: Int
            val bitmap: Bitmap

            if (iconRes != 0) {
                val drawable = ContextCompat.getDrawable(context, iconRes)!!
                val screenWidthPx = windowInfo.containerSize.width
                val windowWidthDp = with(density) { screenWidthPx.toDp() }
                val scaledDpSize = (windowWidthDp * 0.18f).coerceAtLeast(64.dp)

                targetSizePx = with(density) { scaledDpSize.toPx() }.toInt()
                bitmap = createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, targetSizePx, targetSizePx)
                drawable.draw(canvas)
            } else {
                targetSizePx = 1
                bitmap = createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
            }

            val iconShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            runtimeShader.setInputShader("uIcon", iconShader)
            runtimeShader.setFloatUniform("uIconNativeSize", targetSizePx.toFloat(), targetSizePx.toFloat())
            true
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind {
                    runtimeShader.setFloatUniform("uResolution", size.width, size.height)
                    runtimeShader.setFloatUniform("uTime", uTime)
                    drawRect(shaderBrush)
                }
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            content()
        }
    }
}