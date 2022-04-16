import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Crawler {
    // Объект синхронизации
    private static final Object monitor = new Object();
    private static final BlockingQueue queue = new BlockingQueue(monitor);
    // Количество потоков
    private static final int threadsCount = 5;
    // Максимальная глубина поиска
    private static final int maxDepth = 3;
    // Массив, хранящий пары ссылка - глубина
    private static final List<URLDepthPair> store = new ArrayList<>();
    // Количество работающих потоков
    private static int workingThreads = 0;


    public static void main(String[] args) {

        for (int i = 0; i < threadsCount; i++) {
            new Thread(new MyRunnable()).start();
        }
        URLDepthPair firstCell = new URLDepthPair("http://autozakaz25.ru/", 1);
        queue.add(firstCell);
        store.add(firstCell);

    }

    static class MyRunnable implements Runnable {

        @Override
        public void run() {
            try {
                URLDepthPair cell;
                synchronized (monitor) {
                    // Пока очередь пуста текущий поток будет остановлен
                    // Когда вызовется notify он продолжит выполнение с этого места
                    while (queue.getLength() == 0) monitor.wait();
                    // Берем ссылку
                    cell = queue.get();
                    // Наращиваем переменную, которая отвечает за количество потоков, которые находятся в непосредственной работе
                    workingThreads++;
                }

                if (cell.getDepth() < maxDepth) {
                    String html = MyRequest.httpsRequest(cell.getURL());
                    // List со ссылками
                    List<String> allMatches = new ArrayList<>();
                    Matcher m = Pattern.compile("<a\\s+(?:[^>]*?\\s+)?href=([\"'])(.*?)\\1").matcher(html);
                    while (m.find()) {
                        allMatches.add(m.group(2));
                    }
                    for (String link : allMatches) {
                        // делаем ссылку глобальной
                        if (link.startsWith("/"))
                            link = cell.getURL() + link;

                        boolean isExist = false;

                        // Проходимся по всем ссылкам
                        for (URLDepthPair exCell : store) {
                            // Если такая уже есть - пропускаем
                            if (link.equals(exCell.getURL())) {
                                isExist = true;
                                break;
                            }
                        }
                        // Если существует - пропускаем, если нет - добавляем в store и в очередь
                        if (!isExist) {
                            System.out.println(cell.getDepth() + 1 + " " + link);
                            URLDepthPair firstCell = new URLDepthPair(link, cell.getDepth() + 1);

                            queue.add(firstCell);
                            store.add(firstCell);
                        }
                    }

                }

            } catch (Exception ignored) {
                System.out.println(ignored.getMessage());
            } finally {
                synchronized (monitor) {
                    workingThreads--;

                    if (workingThreads == 0 && queue.isEmpty()) {
                        System.exit(1);

                    } else {
                        new Thread(new MyRunnable()).start();
                    }
                }
            }
        }
    }

}