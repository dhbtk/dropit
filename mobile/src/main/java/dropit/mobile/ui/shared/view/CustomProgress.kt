package dropit.mobile.ui.shared.view

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.appcompat.widget.AppCompatImageView
import android.util.AttributeSet
import dropit.mobile.R

class CustomProgress(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    init {
        setImageResource(R.drawable.progress_anim)

        // ref. https://stackoverflow.com/a/53959301
        AnimatedVectorDrawableCompat.registerAnimationCallback(
            drawable,
            object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    postOnAnimationDelayed({
                        (drawable as Animatable).start()
                    }, 650)
                }
            }
        )
        (drawable as Animatable).start()
    }
}
