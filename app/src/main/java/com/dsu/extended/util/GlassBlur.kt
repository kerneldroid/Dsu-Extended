package com.dsu.extended.util

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

private const val GLASS_BLUR_SKSL = """
uniform shader content;
uniform float blurRadius;

half4 main(float2 fragCoord) {
if (blurRadius <= 0.0) {
return content.eval(fragCoord);
}

half4 accum = half4(0.0);
float totalWeight = 0.0;

float noise = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);

const int SAMPLES = 12;
const float GOLDEN_ANGLE = 2.39996323;

for (int i = 0; i < SAMPLES; i++) {
float r = sqrt((float(i) + 0.5) / float(SAMPLES));
float theta = float(i) * GOLDEN_ANGLE + noise;

float2 offset = float2(cos(theta), sin(theta)) * (r * blurRadius);
float weight = exp(-2.0 * r * r);

accum += content.eval(fragCoord + offset) * weight;
totalWeight += weight;
}

return accum / totalWeight;
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object GlassShaderHolder {
    val shader = RuntimeShader(GLASS_BLUR_SKSL)
}

fun Modifier.agslGlassBlur(blurRadius: Float = 20f): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && blurRadius > 0f) {
        this.graphicsLayer {
            val runtimeShader = GlassShaderHolder.shader
            runtimeShader.setFloatUniform("blurRadius", blurRadius)
            renderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, "content")
                .asComposeRenderEffect()
        }
    } else {
        this
    }
