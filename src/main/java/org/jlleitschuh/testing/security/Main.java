package org.jlleitschuh.testing.security;

import com.gradle.publish.upload.Uploader;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String... args) throws IOException {
        String url = "https://s3.amazonaws.com/plugins-artifacts.gradle.org/gradle.plugin.org.jlleitschuh.testing.security/gradle-testing/0.4.25/e508b735e0112058836e46641b2447079dc28a0e74da45aa118995b93d203594/gradle-testing-0.4.25.jar?x-amz-acl=public-read&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20181116T031528Z&X-Amz-SignedHeaders=host&X-Amz-Expires=3599&X-Amz-Credential=AKIAJSORG5G77OJXTM7Q%2F20181116%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=fa23209e48af5fad9cf930d07ad722b902f5dcb3416026ae76e314867dd96bc6";
        File file = new File("build/libs/gradle-testing-0.4.25.jar");
        if (!file.exists()) {
            throw new IllegalStateException("File doesn't exist");
        }
        System.out.println("Uploading!");
        Uploader.putFile(file, url);
    }
}
