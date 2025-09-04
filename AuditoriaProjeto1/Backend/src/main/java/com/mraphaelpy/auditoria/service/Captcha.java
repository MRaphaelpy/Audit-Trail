package com.mraphaelpy.auditoria.service;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Properties;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class Captcha{

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
        // gera o texto
        String captchaText = defaultKaptcha.createText();

        // gera a imagem
        BufferedImage captchaImage = defaultKaptcha.createImage(captchaText);

        // converte a imagem em base64 para enviar ao frontend
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(captchaImage, "jpg", baos);
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

        // retorna objeto com texto e imagem
        return new CaptchaData(captchaText, base64Image);
    }

    // Classe simples só para carregar os dados
    public static class CaptchaData {
        private final String text;  // você salva em sessão ou no banco
        private final String image; // você manda para o frontend em base64

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
