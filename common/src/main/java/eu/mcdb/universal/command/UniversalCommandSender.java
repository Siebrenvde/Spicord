/*
 * Copyright (C) 2019  OopsieWoopsie
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package eu.mcdb.universal.command;

import eu.mcdb.universal.player.UniversalPlayer;

public abstract class UniversalCommandSender {

    private UniversalPlayer player;

    @Deprecated
    protected void setPlayer(UniversalPlayer player) {
        this.player = player;
    }

    /**
     * Get the sender name.
     * 
     * @return the name
     */
    public String getName() {
        return isPlayer() ? player.getName() : "Console";
    }

    /**
     * Get the player who executed the command.
     * 
     * @return the player, may be null
     * @see {@link #isPlayer()}
     */
    @Deprecated
    public UniversalPlayer getPlayer() {
        return player;
    }

    /**
     * Send a message to this sender.
     * 
     * @param message the message to be sent
     */
    public abstract void sendMessage(String message);

    /**
     * Send a formatted message to this sender,
     * using '&' as the color char.
     * 
     * @param message the message to be sent
     */
    public void sendFormattedMessage(String message, Object... args) {
        if (message != null)
            this.sendFormattedMessage(message, '&', args);
    }

    /**
     * Send a formatted message to this sender,
     * using a color/format char of your choice.
     * 
     * Using {@link String#format(String, Object...)} internally.
     * 
     * @param message   the message to be sent
     * @param magicChar the magic char
     * @param args      the arguments referenced by the message value
     */
    public void sendFormattedMessage(String message, char magicChar, Object... args) {
        if (message != null)
            this.sendMessage(String.format(message, args).replace(magicChar, '§'));
    }

    /**
     * Check if the sender has the given permission.
     * 
     * @param permission the permission
     * @return true if the sender has the given permission
     */
    public abstract boolean hasPermission(String permission);

    /**
     * Check if the sender is a player.
     * 
     * @return true if the sender is a player
     */
    @Deprecated
    public boolean isPlayer() {
        return player != null;
    }
}
