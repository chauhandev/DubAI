package com.dAdK.dubAI.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum VoiceType {

    // Premium Voices
    AOEDE("Aoede", "Female", "The Storyteller", "Silky narration for smooth storytelling"),
    CALLIRRHOE("Callirrhoe", "Female", "The Confidante", "Warm, friendly conversations"),
    DESPINA("Despina", "Female", "The Velvet Voice", "Comforting warmth for audiobooks"),
    KORE("Kore", "Female", "The Executive", "Commanding corporate presence"),
    LEDA("Leda", "Female", "The Millennial", "Fresh energy for social media"),
    VINDEMIATRIX("Vindemiatrix", "Female", "The Sage", "Sophisticated wisdom for learning"),
    ZEPHYR("Zephyr", "Female", "The Spark", "Bubbly enthusiasm for commercials"),
    ACHIRD("Achird", "Male", "The Dynamo", "High-energy for action content"),
    ALGENIB("Algenib", "Male", "The Titan", "Thunderous bass for trailers"),
    CHARON("Charon", "Male", "The Authority", "Credible gravitas for news"),
    ENCELADUS("Enceladus", "Male", "The Whisperer", "Intimate ASMR-friendly tones"),
    FENRIR("Fenrir", "Male", "The Hype Beast", "Infectious sports energy"),
    IAPETUS("Iapetus", "Male", "The Neighbor", "Trustworthy warmth for tutorials"),
    ORUS("Orus", "Male", "The Mentor", "Distinguished depth for luxury brands"),
    PUCK("Puck", "Male", "The Charmer", "Playful charisma for comedy"),
    UMBRIEL("Umbriel", "Male", "The Companion", "Easygoing podcast reliability"),

    // Wavenet Voices
    EN_US_MALE("en-US-Wavenet-D", "Male", "Digital Dave", "Simple robotic AI voice"),
    EN_US_FEMALE("en-US-Wavenet-C", "Female", "Synthetic Sarah", "Basic AI voice for utility"),
    EN_GB_FEMALE("en-GB-Wavenet-A", "Female", "Bot Britannia", "Robotic British AI voice"),
    ES_ES_MALE("es-ES-Wavenet-B", "Male", "Robot Rodrigo", "Simple Spanish AI voice"),
    JA_JP_FEMALE("ja-JP-Wavenet-C", "Female", "Bot Yuki", "Basic Japanese AI voice");

    private final String voiceIdentifier;
    private final String gender;
    private final String displayName;
    private final String specialization;

    VoiceType(String voiceIdentifier, String gender, String displayName, String specialization) {
        this.voiceIdentifier = voiceIdentifier;
        this.gender = gender;
        this.displayName = displayName;
        this.specialization = specialization;
    }

    public String getVoiceIdentifier() {
        return voiceIdentifier;
    }

    public String getGender() {
        return gender;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSpecialization() {
        return specialization;
    }

    @JsonCreator
    public static VoiceType fromString(String value) {
        for (VoiceType vt : VoiceType.values()) {
            if (vt.voiceIdentifier.equals(value)) {
                return vt;
            }
        }
        throw new IllegalArgumentException("Unknown voiceType: " + value);
    }
}
