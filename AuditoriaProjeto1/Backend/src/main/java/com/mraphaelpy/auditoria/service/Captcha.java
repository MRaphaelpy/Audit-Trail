package com.mraphaelpy.auditoria.service;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Properties;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class Captcha {

    private final DefaultKaptcha defaultKaptcha;

    public Captcha() {
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", "no");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.textproducer.char.space", "5");

        Config config = new Config(properties);
        this.defaultKaptcha = new DefaultKaptcha();
        this.defaultKaptcha.setConfig(config);
    }

    public CaptchaData generateCaptcha() throws Exception {

        String captchaText = defaultKaptcha.createText();

        BufferedImage captchaImage = defaultKaptcha.createImage(captchaText);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(captchaImage, "jpg", baos);
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

        return new CaptchaData(captchaText, base64Image);
    }

    public static class CaptchaData {
        private final String text;
        private final String image;

        public CaptchaData(String text, String image) {
            this.text = text;
            this.image = image;
        }

        public String getText() {
            return text;
        }

        public String getImage() {
            return image;
        }
    }
}
