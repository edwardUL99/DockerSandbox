/*
 * Copyright 2021 Edward Lynch-Milner
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.eddy.docker.api;

import com.eddy.docker.api.components.Profile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides utilities for the library
 */
public final class Utils {
    /**
     * Load the JSONArray of profiles into a list of profiles
     * @param profiles the array of JSON profiles
     * @return list of parsed Profile objects
     */
    @SuppressWarnings("unchecked")
    public static List<Profile> profilesFromJSON(JSONArray profiles) {
        List<Profile> profiles1 = new ArrayList<>(profiles.size());

        for (Object o : profiles) {
            JSONObject profile = (JSONObject) o;
            Profile.Limits limits;
            if (profile.containsKey("limits")) {
                JSONObject limitsMap = (JSONObject) profile.get("limits");
                limits = new Profile.Limits((Long) limitsMap.getOrDefault("cpuCount", Profile.Limits.CPU_COUNT_DEFAULT),
                        (Long) limitsMap.getOrDefault("memory", Profile.Limits.MEMORY_DEFAULT),
                        (Long)limitsMap.getOrDefault("timeout", Profile.Limits.TIMEOUT_DEFAULT));
            } else {
                limits = new Profile.Limits();
            }

            String profileName = (String)profile.get("name");
            String imageName = (String)profile.getOrDefault("image", "NO-IMAGE-SPECIFIED");
            profiles1.add(new Profile(profileName, imageName, (String)profile.getOrDefault("container-name", imageName),
                    (String)profile.getOrDefault("user", "root"), limits,
                    (Boolean)profile.getOrDefault("networkDisabled", false)));
        }

        return profiles1;
    }
}
