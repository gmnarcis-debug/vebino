package android.webkit;

public class TestSslErrorHandler extends SslErrorHandler {
    public boolean proceeded = false;
    public boolean cancelled = false;

    public TestSslErrorHandler() {
        super();
    }

    @Override
    public void proceed() {
        proceeded = true;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
