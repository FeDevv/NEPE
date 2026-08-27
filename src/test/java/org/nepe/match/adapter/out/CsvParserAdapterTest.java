package org.nepe.match.adapter.out;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.match.port.out.RawCsvMatchRow;
import org.nepe.shared.exception.DataImportException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CsvParserAdapter Unit Tests")
class CsvParserAdapterTest {

    private CsvParserAdapter parser;

    @BeforeEach
    void setUp() {
        parser = new CsvParserAdapter();
    }

    @Nested
    @DisplayName("Real CSV Ingestion Tests (E0.csv & I1.csv)")
    class RealCsvTests {

        @Test
        @DisplayName("Should correctly parse real Premier League CSV file (E0.csv)")
        void shouldParsePremierLeagueCsv() throws IOException {
            File file = new File("E0.csv");
            assertThat(file).exists();

            try (InputStream in = new FileInputStream(file)) {
                List<RawCsvMatchRow> rows = parser.parseCsv(in);
                assertThat(rows).isNotEmpty().hasSize(10);

                RawCsvMatchRow firstRow = rows.getFirst();
                assertThat(firstRow.div()).isEqualTo("E0");
                assertThat(firstRow.dateStr()).isEqualTo("21/08/2026");
                assertThat(firstRow.timeStr()).isEqualTo("20:00");
                assertThat(firstRow.homeTeamRaw()).isEqualTo("Arsenal");
                assertThat(firstRow.awayTeamRaw()).isEqualTo("Coventry");
                assertThat(firstRow.fthg()).isEqualTo(3);
                assertThat(firstRow.ftag()).isEqualTo(0);
                assertThat(firstRow.hs()).isEqualTo(20);
                assertThat(firstRow.as()).isEqualTo(4);
                assertThat(firstRow.hst()).isEqualTo(6);
                assertThat(firstRow.ast()).isEqualTo(1);
                assertThat(firstRow.hr()).isEqualTo(0);
                assertThat(firstRow.ar()).isEqualTo(0);
                assertThat(firstRow.oddsHome()).isEqualTo(1.19); // AvgH
                assertThat(firstRow.oddsDraw()).isEqualTo(6.77); // AvgD
                assertThat(firstRow.oddsAway()).isEqualTo(14.19); // AvgA
            }
        }

        @Test
        @DisplayName("Should correctly parse real Serie A CSV file (I1.csv)")
        void shouldParseSerieACsv() throws IOException {
            File file = new File("I1.csv");
            assertThat(file).exists();

            try (InputStream in = new FileInputStream(file)) {
                List<RawCsvMatchRow> rows = parser.parseCsv(in);
                assertThat(rows).isNotEmpty().hasSize(10);

                RawCsvMatchRow firstRow = rows.getFirst();
                assertThat(firstRow.div()).isEqualTo("I1");
                assertThat(firstRow.dateStr()).isEqualTo("22/08/2026");
                assertThat(firstRow.timeStr()).isEqualTo("17:30");
                assertThat(firstRow.homeTeamRaw()).isEqualTo("Inter");
                assertThat(firstRow.awayTeamRaw()).isEqualTo("Monza");
                assertThat(firstRow.fthg()).isEqualTo(4);
                assertThat(firstRow.ftag()).isEqualTo(1);
                assertThat(firstRow.hs()).isEqualTo(13);
                assertThat(firstRow.as()).isEqualTo(7);
                assertThat(firstRow.hst()).isEqualTo(5);
                assertThat(firstRow.ast()).isEqualTo(3);
                assertThat(firstRow.oddsHome()).isEqualTo(1.19);
            }
        }
    }

    @Nested
    @DisplayName("Validation and Fallback Tests")
    class ValidationAndFallbackTests {

        @Test
        @DisplayName("Should fallback to B365 odds when Avg odds are missing")
        void shouldFallbackToB365Odds() {
            String csv = """
                    Div,Date,HomeTeam,AwayTeam,FTHG,FTAG,B365H,B365D,B365A
                    I1,10/09/2026,Milan,Juventus,1,0,2.40,3.20,3.00
                    """;

            List<RawCsvMatchRow> rows = parser.parseCsvContent(csv);
            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().oddsHome()).isEqualTo(2.40);
            assertThat(rows.getFirst().oddsDraw()).isEqualTo(3.20);
            assertThat(rows.getFirst().oddsAway()).isEqualTo(3.00);
        }

        @Test
        @DisplayName("Should throw DataImportException when mandatory header is missing")
        void shouldThrowWhenMandatoryHeaderMissing() {
            String csv = """
                    Date,HomeTeam,AwayTeam,FTHG,FTAG
                    10/09/2026,Milan,Juventus,1,0
                    """;

            assertThatThrownBy(() -> parser.parseCsvContent(csv))
                    .isInstanceOf(DataImportException.class)
                    .hasMessageContaining("Div");
        }

        @Test
        @DisplayName("Should throw DataImportException when CSV is empty")
        void shouldThrowWhenCsvIsEmpty() {
            assertThatThrownBy(() -> parser.parseCsvContent("   "))
                    .isInstanceOf(DataImportException.class);
        }
    }
}
