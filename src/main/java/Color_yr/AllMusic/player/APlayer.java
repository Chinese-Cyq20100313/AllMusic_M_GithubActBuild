package Color_yr.AllMusic.player;

import Color_yr.AllMusic.player.decoder.BuffPack;
import Color_yr.AllMusic.player.decoder.IDecoder;
import Color_yr.AllMusic.player.decoder.flac.DataFormatException;
import Color_yr.AllMusic.player.decoder.flac.FlacDecoder;
import Color_yr.AllMusic.player.decoder.mp3.Mp3Decoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import javax.sound.sampled.AudioFormat;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class APlayer {

    private HttpClient client;
    private IDecoder decoder;
    private boolean isClose;
    private ChannelManager sndSystem;
    private ChannelManager.Entry channelmanager;
    private AudioFormat audioformat;
    private int index;
    private boolean init;

    public APlayer() {
        try {
            SoundHandler handler = Minecraft.getInstance().getSoundHandler();
            SoundEngine soundManager = ObfuscationReflectionHelper.getPrivateValue(SoundHandler.class, handler, "field_147694_f");
            sndSystem = ObfuscationReflectionHelper.getPrivateValue(SoundEngine.class, soundManager, "field_217941_k");
            client = HttpClientBuilder.create().useSystemProperties().build();
            isClose = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SetMusic(URL url) throws Exception {
        synchronized (this) {
            try {
                decoder = new FlacDecoder();
                decoder.set(client, url);
            } catch (DataFormatException e) {
                decoder = new Mp3Decoder();
                decoder.set(client, url);
            }
            audioformat = new AudioFormat(decoder.getOutputFrequency(),
                    16,
                    decoder.getOutputChannels(),
                    true,
                    false);
            if (channelmanager == null) {
                channelmanager = sndSystem.createChannel(SoundSystem.Mode.STREAMING);
                channelmanager.runOnSoundExecutor((ex) -> {
                    index = ObfuscationReflectionHelper.getPrivateValue(SoundSource.class, ex, "field_216441_b");
                    init = true;
                });
            }
            while (!init) ;
            isClose = false;
        }
    }

    public void play() throws Exception {
        while (true) {
            try {
                if (isClose)
                    break;

                BuffPack output = decoder.decodeFrame();
                if (output == null)
                    break;

                // Stream buffers can only be queued for streaming sources:

                ByteBuffer byteBuffer = (ByteBuffer) BufferUtils.createByteBuffer(
                        output.len).put(output.buff, 0, output.len).flip();

                IntBuffer intBuffer;

                // Clear out any previously queued buffers:
                int processed = AL10.alGetSourcei(index,
                        AL10.AL_BUFFERS_PROCESSED);
                if (processed > 0) {
                    intBuffer = BufferUtils.createIntBuffer(processed);
                    AL10.alGenBuffers(intBuffer);
                    AL10.alSourceUnqueueBuffers(index, intBuffer);
                    AL10.alIsBuffer(intBuffer.get(0));
                } else {
                    intBuffer = BufferUtils.createIntBuffer(1);
                    AL10.alGenBuffers(intBuffer);
                }

                int soundFormat = 0;
                if (audioformat.getChannels() == 1) {
                    if (audioformat.getSampleSizeInBits() == 8) {
                        soundFormat = AL10.AL_FORMAT_MONO8;
                    } else if (audioformat.getSampleSizeInBits() == 16) {
                        soundFormat = AL10.AL_FORMAT_MONO16;
                    } else {
                        return;
                    }
                } else if (audioformat.getChannels() == 2) {
                    if (audioformat.getSampleSizeInBits() == 8) {
                        soundFormat = AL10.AL_FORMAT_STEREO8;
                    } else if (audioformat.getSampleSizeInBits() == 16) {
                        soundFormat = AL10.AL_FORMAT_STEREO16;
                    } else {
                        return;
                    }
                } else {
                    return;
                }

                AL10.alBufferData(intBuffer.get(0), soundFormat, byteBuffer, (int) audioformat.getSampleRate());

                AL10.alSourceQueueBuffers(index, intBuffer);
                AL10.alSourcef(index,AL10.AL_GAIN,Minecraft.getInstance().gameSettings.getSoundLevel(SoundCategory.MASTER));
                if (AL10.alGetSourcei(index,
                        AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                    AL10.alSourcePlay(index);
                }

            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        while (AL10.alGetSourcei(index,
                AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING) {
            Thread.sleep(10);
        }
        if (!isClose)
            close();
    }

    public void close() throws Exception {
        isClose = true;
        AL10.alSourceStop(index);
        int m_numqueued = AL10.alGetSourcei(index, AL10.AL_BUFFERS_QUEUED);
        while (m_numqueued > 0) {
            int temp = AL10.alSourceUnqueueBuffers(index);
            AL10.alDeleteBuffers(temp);
            m_numqueued--;
        }
        if (decoder != null)
            decoder.close();
    }
}
