/*
 *  This file is a part of project QuickShop, the name is PasteInterface.java
 *  Copyright (C) Ghost_chu and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.ghostchu.quickshop.util.paste;

import org.jetbrains.annotations.NotNull;

public interface PasteInterface {
    /**
     * Paste the text to pastebin
     *
     * @param text The text need to paste
     * @return The paste link
     * @throws Exception IOException if paste failed
     */
    String pasteTheText(@NotNull String text) throws Exception;

    /**
     * Paste the text to pastebin
     *
     * @param text The text need to paste
     * @return The paste index
     * @throws Exception IOException if paste failed
     */
    String pasteTheTextJson(@NotNull String text) throws Exception;

}
