/*
 * Copyright 2012-2013 Continuuity,Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.continuuity.weave.filesystem;

import java.net.URI;

/**
 * Factory for creating instance of {@link Location}.
 */
public interface LocationFactory {

  /**
   * Creates an instance of {@link Location} of the given path.
   * @param path The path representing the location.
   * @return An instance of {@link Location}.
   */
  Location create(String path);

  /**
   * Creates an instance of {@link Location} based on {@link java.net.URI} <code>uri</code>.
   *
   * @param uri to the resource on the filesystem.
   * @return An instance of {@link Location}
   */
  Location create(URI uri);
}
