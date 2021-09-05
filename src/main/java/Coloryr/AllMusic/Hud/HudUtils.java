package Coloryr.AllMusic.Hud;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class HudUtils {
    public String Info = "";
    public String List = "";
    public String Lyric = "";
    public SaveOBJ save;
    private ByteBuffer byteBuffer;
    private int textureID = -1;
    public boolean haveImg;
    public final Object lock = new Object();

    public void Set(String data) {
        synchronized (lock) {
            save = new Gson().fromJson(data, SaveOBJ.class);
        }
    }

    public void stop() {
        haveImg = false;
        Info = List = Lyric = "";
    }

    public void SetImg(String picUrl) {

        if (picUrl != null) {
            try {
                URL url = new URL(picUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(4 * 1000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36 Edg/84.0.522.52");
                connection.setRequestProperty("Host", "music.163.com");
                connection.connect();
                InputStream inputStream = connection.getInputStream();
                BufferedImage image = ImageIO.read(inputStream);
                int[] pixels = new int[image.getWidth() * image.getHeight()];
                image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
                byteBuffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);

                for (int h = 0; h < image.getHeight(); h++) {
                    for (int w = 0; w < image.getWidth(); w++) {
                        int pixel = pixels[h * image.getWidth() + w];

                        byteBuffer.put((byte) ((pixel >> 16) & 0xFF));
                        byteBuffer.put((byte) ((pixel >> 8) & 0xFF));
                        byteBuffer.put((byte) (pixel & 0xFF));
                        byteBuffer.put((byte) ((pixel >> 24) & 0xFF));
                    }
                }

                ((Buffer) byteBuffer).flip();
                inputStream.close();
                Thread.sleep(500);
                Minecraft.getInstance().execute(() -> {
                    if (textureID == -1) {
                        textureID = GL11.glGenTextures();
                    }
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, byteBuffer);

                    GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_NEAREST);
                    haveImg = true;
                });
            } catch (Exception e) {
                e.printStackTrace();
                haveImg = false;
            }
        }
    }

    public void update() {
        FontRenderer hud = Minecraft.getInstance().fontRenderer;
        if (save == null)
            return;
        synchronized (lock) {
            if (save.isEnableInfo() && !Info.isEmpty()) {
                int offset = 0;
                String[] temp = Info.split("\n");
                for (String item : temp) {
                    hud.drawStringWithShadow(item, save.getInfo().getX(),
                            save.getInfo().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
            if (save.isEnableList() && !List.isEmpty()) {
                String[] temp = List.split("\n");
                int offset = 0;
                for (String item : temp) {
                    hud.drawStringWithShadow(item, save.getList().getX(),
                            save.getList().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
            if (save.isEnableLyric() && !Lyric.isEmpty()) {
                String[] temp = Lyric.split("\n");
                int offset = 0;
                for (String item : temp) {
                    hud.drawStringWithShadow(item, save.getLyric().getX(),
                            save.getLyric().getY() + offset, 0xffffff);
                    offset += 10;
                }
            }
            if (save.isEnablePic() && haveImg) {
                GlStateManager.bindTexture(textureID);
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glPushMatrix();
                GL11.glTranslatef((float) save.getPic().getX(), (float) save.getPic().getY(), 0.0f);
                GL11.glBegin(7);
                GL11.glTexCoord2f(0.0f, 0.0f);
                GL11.glVertex3f(0.0f, 0.0f, 0.0f);
                GL11.glTexCoord2f(0.0f, 1.0f);
                GL11.glVertex3f(0.0f, (float) 70, 0.0f);
                GL11.glTexCoord2f(1.0f, 1.0f);
                GL11.glVertex3f((float) 70, (float) 70, 0.0f);
                GL11.glTexCoord2f(1.0f, 0.0f);
                GL11.glVertex3f((float) 70, 0.0f, 0.0f);
                GL11.glEnd();
                GL11.glPopMatrix();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableAlphaTest();
            }
        }
    }
}