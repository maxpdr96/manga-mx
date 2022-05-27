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

        String title = docCustomConn.select("h1.title").text().replaceAll("[^A-Za-z0-9\\s]", "");
        Elements chapters = docCustomConn.select("div.chapters span.btn-caps");

        log.info("Manga that will download: {}", title);
        String fileName = dirDownload + "/" + title;

        File theDir = new File(fileName);

        for (int i = chapters.size() - 1; i >= 0; i--) {
            listReverse.add(chapters.get(i).text());
        }

        if (!theDir.exists()) {
            theDir.mkdirs();
        }

        ForkJoinPool myPool = new ForkJoinPool(3);
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
                                        final Pattern pattern = Pattern.compile("(.*\\.)(.*)", Pattern.CASE_INSENSITIVE);
                                        String urlImage = element.absUrl("src");
                                        final Matcher matcher = pattern.matcher(urlImage);
                                        try (InputStream in = new URL(urlImage).openStream()) {
                                            if (matcher.find()) {
                                                Files.copy(in, Paths.get(theDirChapters + "/" + element.attr("id") + "." + matcher.group(2)));
                                            } else {
                                                Files.copy(in, Paths.get(theDirChapters + "/" + element.attr("id") + ".png"));
                                            }
                                            log.info("Image: {}", urlImage);
                                        } catch (Exception e) {
                                            log.error("Erro", e);
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
