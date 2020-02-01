/*
 * Copyright (C) 2020  OopsieWoopsie
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

package eu.mcdb.spicord.api.services.linking;

import java.util.UUID;
import eu.mcdb.universal.player.UniversalPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PendingLinkData {

    private final String name;
    private final UUID uniqueId;

    public LinkData create(long id) {
        return new LinkData(id, name, uniqueId.toString());
    }

    public static PendingLinkData of(UniversalPlayer player) {
        return new PendingLinkData(player.getName(), player.getUniqueId());
    }
}
