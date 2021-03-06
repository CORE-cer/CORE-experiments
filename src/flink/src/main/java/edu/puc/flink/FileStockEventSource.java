package edu.puc.flink;

import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

public class FileStockEventSource extends RichParallelSourceFunction<Event> {
    private final String fileName;
    public static int events = 0;


    FileStockEventSource(String streamFileName){
        fileName = streamFileName;
    }

    @Override
    public void run(SourceContext<Event> ctx) throws Exception {
        if (!Stock.memoryTest) {
            try {
                int timeout = Stock.timeout;
                FileReader file = new FileReader(fileName);
                BufferedReader reader = new BufferedReader(file);
                LinkedList<Event> eventList = new LinkedList<>();
                long start = System.nanoTime();
                String line;

                while ((line = reader.readLine()) != null){
                    StockEvent event = StockEvent.getEventFromString(line);
                    eventList.add(event);
                }

                (new Thread(() -> {
                    try {
                        Thread.sleep(timeout * 1000000L);
                        System.out.print((double)(System.nanoTime() - start)/1000000000 + ",");
                        System.out.print(events + ",");
                        System.out.print((double)Stock.enumerationTime/1000000000 + ",");
                        System.out.print(Stock.totalResults);
                        System.out.println();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })).start();


                while (!eventList.isEmpty()) {
                    events++;
                    Event event = eventList.removeFirst();
                    ctx.collect(event);
                    if (timeout != 0 && System.nanoTime() - start >= timeout * 1000000000L) {
                        break;
                    }
                }
                ctx.close();

                try {
                    reader.close();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            catch (FileNotFoundException | NullPointerException ex){
                ex.printStackTrace();
            }}
        else {
            try {
                int timeout = Stock.timeout;
                FileReader file = new FileReader(fileName);
                BufferedReader reader = new BufferedReader(file);
                long start = System.nanoTime();
                String line;

                long total = 0;
                long tmp;


                (new Thread(() -> {
                    try {
                        Thread.sleep(timeout * 1000L);
                        if (Stock.count == 0) {
                            Stock.count = 1;
                        }
                        System.out.print(Stock.maxMemTotal + ",");
                        System.out.print(Stock.avgMemTotal/Stock.count + ",");
                        System.out.print(Stock.maxMemUsed + ",");
                        System.out.println(Stock.avgMemUsed/Stock.count);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })).start();

                while ((line = reader.readLine()) != null) {
                    if (Main.maxEvents != 0 && events >= Main.maxEvents) {
                        break;
                    }
                    StockEvent event = StockEvent.getEventFromString(line);
                    tmp = System.nanoTime();
                    ctx.collect(event);
                    total += System.nanoTime() - tmp;
                    events++;
                    if (events % 10000 == 0) {
                        Stock.avgMemTotal = Runtime.getRuntime().totalMemory();
                        Stock.avgMemTotal += Stock.avgMemTotal;
                        if (Stock.avgMemTotal > Stock.maxMemTotal) {
                            Stock.maxMemTotal = Stock.avgMemTotal;
                        }
                        System.gc();
                        Stock.avgMemUsed = Stock.avgMemTotal - Runtime.getRuntime().freeMemory();
                        Stock.avgMemUsed += Stock.avgMemUsed;
                        if (Stock.avgMemUsed > Stock.maxMemUsed) {
                            Stock.maxMemUsed = Stock.avgMemUsed;
                        }
                        Stock.count++;
                    }
                    if (timeout != 0 && System.nanoTime() - start >= timeout * 1000000000L) {
                        //                    System.err.println(event.getId());
                        break;
                    }
                }


                ctx.close();

                //        System.out.print((((double)compileTime) / 1000000000) + ",");
                //        System.out.print((((double)total) / 1000000000) + ",");
                //        System.out.print((((double)EventListener.totalTime) / 1000000000) + ",");
                //        System.out.println(EventListener.totalMatches);

                //            System.out.println("context collection time: " + (total / 1000000));
                Stock.executionTime = total;
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (FileNotFoundException | NullPointerException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void cancel() { }
}