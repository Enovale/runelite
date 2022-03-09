/*
 * Copyright (c) 2019, Spedwards <https://github.com/Spedwards>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.profiles;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.game.WorldService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;

@Slf4j
class ProfilePanel extends JPanel
{
    private static final ImageIcon DELETE_ICON;
    private static final ImageIcon DELETE_HOVER_ICON;

    static
    {
        final BufferedImage deleteImg = ImageUtil.loadImageResource(ProfilesPlugin.class, "delete_icon.png");
        DELETE_ICON = new ImageIcon(deleteImg);
        DELETE_HOVER_ICON = new ImageIcon(ImageUtil.alphaOffset(deleteImg, -100));
    }

    public final String Label;
    private final String loginText;
    private final String worldText;
    private final String passwordText;

    private final WorldService worldService;

    ProfilePanel(ProfilesPanel parent, final Client client, String data, ProfilesConfig config, WorldService worldservice)
    {
        this.worldService = worldservice;
        String[] parts = data.split(":");
        this.Label = parts[0];
        this.loginText = parts[1];
        this.worldText = parts[2];
        this.passwordText = parts[3];

        final ProfilePanel panel = this;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel labelWrapper = new JPanel(new BorderLayout());
        labelWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelWrapper.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR)
        ));

        JPanel panelActions = new JPanel(new BorderLayout(3, 0));
        panelActions.setBorder(new EmptyBorder(0, 0, 0, 8));
        panelActions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel delete = new JLabel();
        delete.setIcon(DELETE_ICON);
        delete.setToolTipText("Delete account profile");
        delete.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                panel.getParent().remove(panel);
                ProfilesPanel.removeProfile(data);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                delete.setIcon(DELETE_HOVER_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                delete.setIcon(DELETE_ICON);
            }
        });

        panelActions.add(delete, BorderLayout.EAST);

        JLabel nameLabel = new JLabel(parts[0]);
        nameLabel.setBorder(null);
        nameLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        nameLabel.setPreferredSize(new Dimension(0, 24));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setBorder(new EmptyBorder(0, 8, 0, 0));
        nameLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                String name = JOptionPane.showInputDialog(null,
                        "Input the new name for this profile: ", "Profiles", JOptionPane.INFORMATION_MESSAGE);

                config.profilesData(config.profilesData().replace(data,
                        data.replace(panel.Label + ":" + loginText + ":" + worldText + ":" + passwordText + ":" + "0",
                                name + ":" + loginText + ":" + worldText + ":" + passwordText + ":" + "0")));
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                nameLabel.setForeground(Color.BLUE);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                nameLabel.setForeground(Color.WHITE);
            }
        });

        JLabel worldLabel = new JLabel("(" + parts[2] + ")");
        worldLabel.setBorder(null);
        worldLabel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        worldLabel.setPreferredSize(new Dimension(0, 24));
        worldLabel.setForeground(Color.WHITE);
        worldLabel.setBorder(new EmptyBorder(0, 8, 0, 0));
        worldLabel.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                String world = JOptionPane.showInputDialog(null,
                        "Input the Default World", "Profiles", JOptionPane.INFORMATION_MESSAGE);

                try {
                    int worldInt = Integer.parseInt(world);
                    config.profilesData(config.profilesData().replace(data,
                            data.replace(panel.Label + ":" + loginText + ":" + worldText + ":" + passwordText + ":" + "0",
                                    panel.Label + ":" + loginText + ":" + worldInt + ":" + passwordText + ":" + "0")));
                } catch (NumberFormatException error) {
                    log.error("Invalid World Format");
                }
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                worldLabel.setForeground(Color.BLUE);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                worldLabel.setForeground(Color.WHITE);
            }
        });

        JPanel titleBar = new JPanel();
        GroupLayout layout = new GroupLayout(titleBar);
        titleBar.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addComponent(nameLabel)
                        .addComponent(worldLabel)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING))
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(nameLabel)
                                .addComponent(worldLabel))
        );
        labelWrapper.add(titleBar, BorderLayout.CENTER);
        labelWrapper.add(panelActions, BorderLayout.EAST);
        nameLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Login(client);
                }
            }
        });

        JPanel bottomContainer = new JPanel(new BorderLayout());
        bottomContainer.setBorder(new EmptyBorder(8, 0, 8, 0));
        bottomContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        bottomContainer.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    Login(client);
                }
            }
        });

        JLabel login = new JLabel();
        login.setText(config.isStreamerMode() ? "Hidden email" : loginText);
        login.setBorder(null);
        login.setPreferredSize(new Dimension(0, 24));
        login.setForeground(Color.WHITE);
        login.setBorder(new EmptyBorder(0, 8, 0, 0));

        bottomContainer.add(login, BorderLayout.NORTH);

        JLabel pass = new JLabel();
        pass.setText(config.isStreamerMode() ? "Hidden Password Length" : String.join("", Collections.nCopies(passwordText.length(), "*")));
        pass.setBorder(null);
        pass.setPreferredSize(new Dimension(0, 24));
        pass.setForeground(Color.WHITE);
        pass.setBorder(new EmptyBorder(0, 8, 0, 0));

        bottomContainer.add(pass, BorderLayout.SOUTH);

        add(labelWrapper, BorderLayout.NORTH);
        add(bottomContainer, BorderLayout.CENTER);
    }

    public void Login(final Client client)
    {
        if (client.getGameState() == GameState.LOGIN_SCREEN) {
            client.setUsername(loginText);
            client.setPassword(passwordText);
            if (worldText.length() == 3) {
                Integer worldInt = Integer.parseInt(worldText);
                if (ProfilesPlugin.CorrectWorld(worldInt) == 0) {
                    log.error("Bad World ID entered into profile " + this.Label + "!");
                    return;
                }
                if (client.getWorld() == worldInt) {
                    log.warn("Already in this world!");
                    return;
                }
                World world = worldService.getWorlds().findWorld(worldInt);
                if (world == null) {
                    return;
                }

                final net.runelite.api.World rsWorld = client.createWorld();
                rsWorld.setActivity(world.getActivity());
                rsWorld.setAddress(world.getAddress());
                rsWorld.setId(world.getId());
                rsWorld.setPlayerCount(world.getPlayers());
                rsWorld.setLocation(world.getLocation());
                rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
                client.changeWorld(rsWorld);
            }
        }
    }
}