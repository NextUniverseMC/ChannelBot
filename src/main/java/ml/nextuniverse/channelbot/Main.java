package ml.nextuniverse.channelbot;

import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.VoiceChannel;
import de.btobastian.javacord.entities.impl.ImplVoiceChannel;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.permissions.*;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import de.btobastian.javacord.listener.voice.UserJoinVoiceChannelListener;
import de.btobastian.javacord.listener.voice.UserLeaveVoiceChannelListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by TheDiamondPicks on 5/08/2017.
 */
public class Main {

    static DiscordAPI api;

    static HashMap<User, Channel> channelOwners = new HashMap<>();
    static HashMap<VoiceChannel, User> voiceChannelOwners = new HashMap<>();

    static HashMap<User, VoiceChannel> currentVoiceChannel = new HashMap<>();

    static HashMap<VoiceChannel, Boolean> hasJoined = new HashMap<>();

    public static void main(String[] args) {
        api = Javacord.getApi("Mjg1MjkxNjYzMjk4MTM0MDE2.DGbhOQ.nr1TlZa9ijg2iZt_JLj45bpJj94", true);
        api.connect(new FutureCallback<DiscordAPI>() {
            @Override
            public void onSuccess(DiscordAPI api) {
                final Server server = api.getServerById("311008027064926208");
                // register listener
                api.registerListener(new MessageCreateListener() {
                    @Override
                    public void onMessageCreate(DiscordAPI api, final Message message) {
                        String name = "";
                        if (message.getContent().split(" ").length == 1) {
                            name = message.getAuthor().getName();
                        }
                        else {
                            String[] strings = message.getContent().split(" ");
                            StringBuilder sb = new StringBuilder();
                            for (int i = 1 ; i < strings.length ; i++) {
                                sb.append(strings[i]);
                            }
                            sb.trimToSize();
                            name = sb.toString().trim();
                        }
                        // check the content of the message
                        if (message.getContent().startsWith("/create")
                                && !message.isPrivateMessage()) {
                            boolean canUse = true;
                            for (Channel c : server.getChannels()) {
                                if (c.getName().equalsIgnoreCase(name)) {
                                    canUse = false;
                                    break;
                                }
                            }
                            if (!canUse) {
                                message.reply("⚠ Channel Already Exists!");
                            }
                            else if (channelOwners.containsKey(message.getAuthor())) {
                                message.reply("⚠ You already have a channel!");
                            }
                            else {
                                Future<VoiceChannel> voiceFuture = server.createVoiceChannel(name);
                                final VoiceChannel voice;
                                try {
                                    voice = voiceFuture.get();
                                } catch (InterruptedException | ExecutionException e) {
                                    System.out.println("Server creation failed!");
                                    e.printStackTrace();
                                    return;
                                }

                                Future<Channel> chatFuture = server.createChannel(name);
                                final Channel chat;
                                try {
                                    chat = chatFuture.get();
                                } catch (InterruptedException | ExecutionException e) {
                                    System.out.println("Server creation failed!");
                                    e.printStackTrace();
                                    return;
                                }



                                PermissionsBuilder builder = api.getPermissionsBuilder();
                                builder.setState(PermissionType.READ_MESSAGES, PermissionState.ALLOWED);
                                builder.setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED);
                                builder.setState(PermissionType.MANAGE_ROLES, PermissionState.ALLOWED);
                                builder.setState(PermissionType.MANAGE_MESSAGES, PermissionState.ALLOWED);

                                chat.updateOverwrittenPermissions(message.getAuthor(), builder.build());

                                chat.updateOverwrittenPermissions(server.getRoleById("311008027064926208"), api.getPermissionsBuilder().setState(PermissionType.READ_MESSAGES, PermissionState.DENIED).build());

                                PermissionsBuilder builder2 = api.getPermissionsBuilder();
                                builder2.setState(PermissionType.VOICE_CONNECT, PermissionState.ALLOWED);
                                builder2.setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED);
                                builder2.setState(PermissionType.MANAGE_ROLES, PermissionState.ALLOWED);
                                builder2.setState(PermissionType.MANAGE_MESSAGES, PermissionState.ALLOWED);


                                voice.updateOverwrittenPermissions(server.getRoleById("311008027064926208"), api.getPermissionsBuilder().setState(PermissionType.VOICE_CONNECT, PermissionState.DENIED).build());

                                voice.updateOverwrittenPermissions(message.getAuthor(), builder2.build());

                                System.out.println(voice.toString());


                                message.reply("Channel Created");

                                chat.sendMessage("Welcome to your personal text channel.\n" +
                                        "You can edit this channel or the voice channel by clicking the cog icon next to them.\n" +
                                        "**Tip!** You can quickly add people to the channel with **/add <name>**\n" +
                                        "If the voice channel is empty for 30 seconds it will be deleted, along with this channel!");

                                channelOwners.put(message.getAuthor(), chat);
                                voiceChannelOwners.put(voice, message.getAuthor());
                                new java.util.Timer().schedule(

                                        new java.util.TimerTask() {
                                            @Override
                                            public void run() {

                                                if (hasJoined.containsKey(voice)) {
                                                    if (!hasJoined.get(voice) && voice.getConnectedUsers().size() == 0) {
                                                        try {
                                                            voice.delete();
                                                        } catch (Exception e) {
                                                        }
                                                        try {
                                                            chat.delete();
                                                        } catch (Exception e) {
                                                        }
                                                    }
                                                }
                                                else {
                                                    if (voice.getConnectedUsers().size() == 0) {
                                                        try {
                                                            voiceChannelOwners.remove(voice);
                                                            voice.delete();
                                                        } catch (Exception e) {
                                                        }
                                                        try {
                                                            channelOwners.remove(message.getAuthor());
                                                            chat.delete();
                                                        } catch (Exception e) {
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        30000
                                );
// chat.updateOverwrittenPermissions()
                            }
                        }
                        else if (message.getContent().startsWith("/add")) {
                            String[] strings = message.getContent().split(" ");
                            if (strings.length != 0) {
                                if (!channelOwners.containsKey(message.getAuthor())) {
                                    message.reply("You must own a channel in order to use this command!");
                                    return;
                                }
                                StringBuilder sb = new StringBuilder();
                                for (User u : message.getMentions()) {
                                    if (sb.length() == 0) {
                                        sb.append(u.getName());
                                    }
                                    else {
                                        sb.append(", " + u.getName());
                                    }
                                    Channel chat = channelOwners.get(message.getAuthor());
                                    PermissionsBuilder builder = api.getPermissionsBuilder();
                                    builder.setState(PermissionType.READ_MESSAGES, PermissionState.ALLOWED);
                                    chat.updateOverwrittenPermissions(u, builder.build());

                                    VoiceChannel voice = null;

                                    for (Map.Entry<VoiceChannel, User> c : voiceChannelOwners.entrySet()) {
                                        VoiceChannel v = c.getKey();
                                        User z = c.getValue();

                                        if (z.equals(message.getAuthor())) {
                                            voice = v;
                                        }
                                    }
                                    PermissionsBuilder build = api.getPermissionsBuilder();
                                    build.setState(PermissionType.VOICE_CONNECT, PermissionState.ALLOWED);

                                    voice.updateOverwrittenPermissions(u, build.build());




                                }
                                sb.trimToSize();
                                String s = sb.toString().trim();
                                message.reply("Added user(s) " + s);
                            }
                        }
                        else if (message.getContent().startsWith("/remove")) {
                            if (!channelOwners.containsKey(message.getAuthor())) {
                                message.reply("You must own a channel in order to use this command!");
                                return;
                            }
                            StringBuilder sb = new StringBuilder();
                            for (User u : message.getMentions()) {
                                if (sb.length() == 0) {
                                    sb.append(u.getName());
                                }
                                else {
                                    sb.append(", " + u.getName());
                                }
                                Channel chat = channelOwners.get(message.getAuthor());
                                chat.deleteOverwrittenPermissions(u);

                                VoiceChannel voice = null;

                                for (Map.Entry<VoiceChannel, User> c : voiceChannelOwners.entrySet()) {
                                    VoiceChannel v = c.getKey();
                                    User z = c.getValue();

                                    if (z.equals(message.getAuthor())) {
                                        voice = v;
                                    }
                                }

                                voice.deleteOverwrittenPermissions(u);

                            }
                            sb.trimToSize();
                            String s = sb.toString().trim();
                            message.reply("Removed user(s) " + s);
                        }
                    }
                });
                api.registerListener(new UserLeaveVoiceChannelListener() {
                    @Override
                    public void onUserLeaveVoiceChannel(DiscordAPI discordAPI, final User user) {
                            if (voiceChannelOwners.containsKey(currentVoiceChannel.get(user))) {
                                final VoiceChannel voiceChannel = currentVoiceChannel.get(user);
                                currentVoiceChannel.remove(user);
                                hasJoined.put(voiceChannel, false);
                                new java.util.Timer().schedule(

                                        new java.util.TimerTask() {
                                            @Override
                                            public void run() {
                                                if (!hasJoined.get(voiceChannel)) {
                                                    User owner;
                                                    boolean delete = false;
                                                    try {
                                                        owner = voiceChannelOwners.get(voiceChannel);
                                                        if (voiceChannel.getConnectedUsers().size() == 0) {
                                                            voiceChannelOwners.remove(voiceChannel);
                                                            voiceChannel.delete();
                                                            delete = true;
                                                        }


                                                    }
                                                    catch (Exception e){return;}
                                                    try {
                                                        Channel chatChannel = channelOwners.get(owner);
                                                        if (delete) {
                                                            chatChannel.delete();
                                                            channelOwners.remove(owner);
                                                        }
                                                    }
                                                    catch (Exception e){}
                                                }
                                            }
                                        },
                                        30000
                                );
                            }
                    }
                });

                api.registerListener(new UserJoinVoiceChannelListener() {
                    @Override
                    public void onUserJoinVoiceChannel(DiscordAPI discordAPI, User user, VoiceChannel voiceChannel) {
                        currentVoiceChannel.put(user, voiceChannel);
                        if (voiceChannelOwners.containsKey(voiceChannel)) {
                            hasJoined.put(voiceChannel, true);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }
}
