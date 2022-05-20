package com.hidarisoft.mangamx;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MangaMxApplication {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        String mangaUrl = args[0];
        String dirDownload = args[1];

        Document docCustomConn = Jsoup.connect(mangaUrl)
                .userAgent("Opera")
                .get();

        List<String> listReverse = new ArrayList<>();

        Elements title = docCustomConn.select("h1.title");
        Elements chapters = docCustomConn.select("div.chapters span.btn-caps");

        log.info("Manga that will download: {}", title.text());
        String fileName = dirDownload + "/" + title.text();

        File theDir = new File(fileName);

        for (int i = chapters.size() - 1; i >= 0; i--) {
            listReverse.add(chapters.get(i).text());
        }

        if (!theDir.exists()) {
            theDir.mkdirs();
        }

        ForkJoinPool myPool = new ForkJoinPool(2);
        long startTime = System.nanoTime();

        myPool.submit(() ->
                listReverse.parallelStream().forEach(s -> {
                            File theDirChapters = new File(fileName + "/" + s);

                            if (!theDirChapters.exists()) {

                                theDirChapters.mkdirs();

                                try {
                                    Thread.sleep(5000);
                                    Document docCustomCon = Jsoup.connect(mangaUrl + "/" + s)
                                            .userAgent("Opera")
                                            .get();


                                    Elements link = docCustomCon.select("div.read-slideshow img");

                                    link.forEach(element -> {
                                        String fileSave;
                                        final Pattern pattern = Pattern.compile("[a-zA-Z]+:\\/\\/([a-zA-Z]+(-[a-zA-Z]+)+)\\.[a-zA-Z]+3\\.[a-zA-Z]+\\/[a-zA-Z]+_[a-zA-Z]+\\/([^\\/]*)\\/([\\S][^\\/]*)\\/\\w*_([0-9]*).([a-z]*)", Pattern.CASE_INSENSITIVE);
                                        final Matcher matcher = pattern.matcher(element.absUrl("src"));

                                        while (matcher.find()) {
                                            log.info("Image link: {}", matcher.group());
                                            try (InputStream in = new URL(matcher.group()).openStream()) {
                                                if (matcher.group(5).isBlank() && matcher.group(6).isBlank()) {
                                                    fileSave = theDirChapters + "/" + matcher.group(9) + "." + matcher.group(10);
                                                } else {
                                                    fileSave = theDirChapters + "/" + matcher.group(5) + "." + matcher.group(6);
                                                }
                                                log.info("Folder you are saving: {}", fileSave);
                                                Files.copy(in, Paths.get(fileSave));
                                            } catch (Exception e) {
                                                log.error("Erro", e);
                                            }
                                        }
                                    });
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                log.info("Already exists: {}", theDirChapters);
                            }
                        }
                )).get();
        long endTime = System.nanoTime();

        long duration = TimeUnit.SECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);

        log.info("The time it took was: {} seconds", duration);
    }

}
