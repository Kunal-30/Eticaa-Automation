package Utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Scroll elements into view automatically where needed.
 */
public final class ScrollHelper {

    private ScrollHelper() {}

    /**
     * Scrolls the element into view (center of viewport) so it is visible and clickable.
     */
    public static void scrollIntoView(WebDriver driver, WebElement element) {
        if (driver == null || element == null) return;
        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'smooth',block:'center',inline:'center'});",
                element
            );
            try { Thread.sleep(300); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            } catch (Exception e2) {
                // ignore
            }
        }
    }

    /**
     * Force-scrolls the window so that the element is very close to the vertical center of the screen.
     * Useful on details pages where nested containers can prevent normal scrollIntoView from centering.
     */
    public static void scrollIntoStrictCenter(WebDriver driver, WebElement element) {
        if (driver == null || element == null) return;
        try {
            String script =
                "const el = arguments[0];" +
                "const rect = el.getBoundingClientRect();" +
                "const viewportHeight = window.innerHeight || document.documentElement.clientHeight;" +
                "const scrollY = window.scrollY || window.pageYOffset;" +
                "const targetY = rect.top + scrollY - (viewportHeight / 2) + (rect.height / 2);" +
                "window.scrollTo({top: targetY, behavior: 'smooth'});";
            ((JavascriptExecutor) driver).executeScript(script, element);
            try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            // Fallback to normal center scroll
            scrollIntoView(driver, element);
        }
    }

    /**
     * Scrolls the element to the top of its scroll container. Use when an expanded section above
     * (e.g. Location) covers the target element (e.g. Designation). Puts target at top so it stays visible.
     */
    public static void scrollIntoViewAtTop(WebDriver driver, WebElement element) {
        if (driver == null || element == null) return;
        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'smooth',block:'start',inline:'nearest'});",
                element
            );
            try { Thread.sleep(400); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            } catch (Exception e2) {
                // ignore
            }
        }
    }
}
