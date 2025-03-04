/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.text;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Utility class for creating {@link Content} objects.
 *
 * @deprecated Use {@link ContentIO} class instead
 * @author Rosemoe
 */
@Deprecated(since = "0.21.1", forRemoval = true)
public class ContentCreator {

    /**
     * Create a {@link Content} from stream.
     * The stream will get closed if the operation is successfully done.
     */
    @NonNull
    public static Content fromStream(@NonNull InputStream stream) throws IOException {
        return ContentIO.createFrom(stream);
    }

    /**
     * Create a {@link Content} from reader.
     * <p>
     * The reader will get closed if the operation is successfully done.
     */
    @NonNull
    public static Content fromReader(@NonNull Reader reader) throws IOException {
        return ContentIO.createFrom(reader);
    }

}
