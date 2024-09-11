package com.coderevolt.log;

import com.coderevolt.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemLogCollect {

    private static volatile boolean isInject = false;

    private static PrintStreamWrapper errStreamWrapper;

    public static PrintStream getErrStreamWrapper() {
        if (errStreamWrapper == null) {
            throw new IllegalStateException("errStreamWrapper has not been initialized");
        }
        return errStreamWrapper;
    }

    /**
     * 输出写出日志
     */
    public static void injectStandardStream() {
        if (!isInject) {
            synchronized (SystemLogCollect.class) {
                if (!isInject) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyMMdd");
                        File logDir = new File(ProjectUtil.homePath, "log/" + dateFormat.format(new Date()));
                        if (!logDir.exists()) logDir.mkdirs();

                        errStreamWrapper = new PrintStreamWrapper(System.err, new File(logDir, "err.log"));
                        System.setOut(new PrintStreamWrapper(System.out, new File(logDir, "info.log")));
                        System.setErr(errStreamWrapper);
                        System.out.println("System stream injected");
                        isInject = true;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    static class PrintStreamWrapper extends PrintStream {

        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        private final BufferedWriter bufferedWriter;

        public PrintStreamWrapper(@NotNull OutputStream out, File logFile) throws IOException {
            super(out, true);
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)));
        }

        @Override
        public void print(boolean b) {
            super.print(b);
            write(String.valueOf(b));
        }

        @Override
        public void print(char c) {
            super.print(c);
            write(String.valueOf(c));
        }

        @Override
        public void print(int i) {
            super.print(i);
            write(String.valueOf(i));
        }

        @Override
        public void print(long l) {
            super.print(l);
            write(String.valueOf(l));
        }

        @Override
        public void print(float f) {
            super.print(f);
            write(String.valueOf(f));
        }

        @Override
        public void print(double d) {
            super.print(d);
            write(String.valueOf(d));
        }

        @Override
        public void print(@NotNull char[] s) {
            super.print(s);
            write(String.valueOf(s));
        }

        @Override
        public void print(@Nullable String s) {
            super.print(s);
            write(s);
        }

        @Override
        public void print(@Nullable Object obj) {
            super.print(obj);
            write(String.valueOf(obj));
        }

        @Override
        public void println() {
            synchronized (bufferedWriter) {
                super.println();
                newLine();
            }
        }

        @Override
        public void println(boolean x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        @Override
        public void println(char x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        @Override
        public void println(int x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        @Override
        public void println(long x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        @Override
        public void println(float x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        @Override
        public void println(double x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        @Override
        public void println(@NotNull char[] x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        @Override
        public void println(@Nullable String x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        @Override
        public void println(@Nullable Object x) {
            synchronized (bufferedWriter) {
                super.println(x);
                newLine();
            }
        }

        private void newLine() {
            synchronized (bufferedWriter) {
                try {
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                } catch (IOException x) {
                    x.printStackTrace(this);
                }
            }
        }

        private void write(String s) {
            try {
                synchronized (bufferedWriter) {
                    bufferedWriter.write("[" + Thread.currentThread().getName() + "]");
                    bufferedWriter.write(dateFormat.format(new Date()));
                    bufferedWriter.write(" : ");
                    bufferedWriter.write(s);
                    if ((s.indexOf('\n') >= 0)) {
                        bufferedWriter.flush();
                    }
                }
            } catch (InterruptedIOException x) {
                Thread.currentThread().interrupt();
            } catch (IOException x) {
                x.printStackTrace(this);
            }
        }

        @Override
        public void close() {
            super.close();
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace(this);
            }
        }
    }

}
