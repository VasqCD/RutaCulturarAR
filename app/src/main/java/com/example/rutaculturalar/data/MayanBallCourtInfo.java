package com.example.rutaculturalar.data;

public class MayanBallCourtInfo {

    public static class CulturalData {
        public final String title;
        public final String description;

        public CulturalData(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    public static CulturalData getMainCourtInfo() {
        return new CulturalData(
            "Campo de Pelota Maya - Copán",
            "Este es uno de los campos de pelota más importantes de la civilización maya. " +
            "Construido entre 738-750 d.C., era el centro de rituales sagrados donde se " +
            "jugaba el juego de pelota ceremonial, representando la lucha entre las fuerzas " +
            "del bien y el mal."
        );
    }

    public static CulturalData getHistoricalContext() {
        return new CulturalData(
            "Contexto Histórico",
            "Durante el reinado de 18 Conejo (695-738 d.C.), Copán alcanzó su máximo " +
            "esplendor. El campo de pelota era usado para ceremonias religiosas y " +
            "rituales de sacrificio que conectaban con el inframundo maya."
        );
    }

    public static CulturalData getArchitecturalDetails() {
        return new CulturalData(
            "Detalles Arquitectónicos",
            "El campo mide aproximadamente 28 metros de largo. Los muros inclinados " +
            "están decorados con cabezas de guacamaya, símbolo del sol y la realeza. " +
            "Los marcadores centrales representan la entrada al inframundo."
        );
    }

    public static CulturalData getGameRules() {
        return new CulturalData(
            "El Juego Sagrado",
            "Los jugadores debían mantener una pelota de caucho en movimiento usando " +
            "solo caderas, codos y hombros. El juego tenía significado cosmológico: " +
            "representaba el movimiento del sol y los planetas."
        );
    }
}
