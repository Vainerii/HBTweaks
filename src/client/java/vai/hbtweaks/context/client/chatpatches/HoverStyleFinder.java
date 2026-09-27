package vai.hbtweaks.context.client.chatpatches;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Exact same thing as ActiveTextCollector:ClickableStyleFinder but with hover
 * I should check it, feels a bit too miraculous
 */
public class HoverStyleFinder implements ActiveTextCollector {
    private static final Parameters INITIAL = new Parameters(new Matrix3x2f());
    private final Font font;
    private final int testX;
    private final int testY;
    private Parameters defaultParameters;
    // private boolean includeInsertions;
    private @Nullable Style result;
    private final Consumer<Style> styleScanner;

    /**
     * @param font the font used to lay out the collected text
     * @param testX the tested x position, in GUI pixels
     * @param testY the tested y position, in GUI pixels
     */
    public HoverStyleFinder(final Font font, final int testX, final int testY) {
        this.defaultParameters = INITIAL;
        this.styleScanner = (style) -> {
            if (style.getHoverEvent() != null) {
                this.result = style;
            }
        };
        this.font = font;
        this.testX = testX;
        this.testY = testY;
    }

    @Override
    public Parameters defaultParameters() {
        return this.defaultParameters;
    }

    @Override
    public void defaultParameters(final ActiveTextCollector.Parameters newParameters) {
        this.defaultParameters = newParameters;
    }

    @Override
    public void accept(final TextAlignment alignment, final int anchorX, final int y, final Parameters parameters, final FormattedCharSequence text) {
        int leftX = alignment.calculateLeft(anchorX, this.font, text);
        GuiTextRenderState renderState = new GuiTextRenderState(this.font, text, parameters.pose(), leftX, y, ARGB.white(parameters.opacity()), 0, true, true, parameters.scissor());
        ActiveTextCollector.findElementUnderCursor(renderState, (float)this.testX, (float)this.testY, this.styleScanner);
    }

    @Override
    public void acceptScrolling(final Component message, final int centerX, final int left, final int right, final int top, final int bottom, final Parameters parameters) {
        int lineWidth = this.font.width(message);
        Objects.requireNonNull(this.font);
        int lineHeight = 9;
        this.defaultScrollingHelper(message, centerX, left, right, top, bottom, lineWidth, lineHeight, parameters);
    }

    //public ClickableStyleFinder includeInsertions(final boolean flag) {
    //    this.includeInsertions = flag;
    //    return this;
    //}

    /**
     * @return the last style with a hover event found under the tested position, or null
     */
    public @Nullable Style result() {
        return this.result;
    }
}
