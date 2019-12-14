package brownshome.modding.util;

import brownshome.modding.ModVersion;
import brownshome.modding.util.SemanticModVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SemanticModVersionTest {
	@Test
	void createVersion() {
		var expected = new SemanticModVersion(1, 2, 3);
		var result = SemanticModVersion.createVersion("1.2.3");

		assertEquals(expected, result);
	}

	@Test
	void createVersionWithRelease() {
		var expected = new SemanticModVersion(1, 2, 3, "98alpha-4.QA5");
		var result = SemanticModVersion.createVersion("1.2.3-98alpha-4.QA5");

		assertEquals(expected, result);
	}

	@Test
	void createVersionWithBuild() {
		var expected = new SemanticModVersion(1, 2, 3, null, "1.34-FA");
		var result = SemanticModVersion.createVersion("1.2.3+1.34-FA");

		assertEquals(expected, result);
	}

	@Test
	void createVersionWithReleaseAndBuild() {
		var expected = new SemanticModVersion(1, 2, 3, "98alpha-4.QA5", "1.34-FA");
		var result = SemanticModVersion.createVersion("1.2.3-98alpha-4.QA5+1.34-FA");

		assertEquals(expected, result);
	}

	@Test
	void isAPICompatibleWithSelf() {
		var self = new SemanticModVersion(1, 0, 0, "alpha");

		assertTrue(self.isCompatibleWith((ModVersion) self));
	}

	@Test
	void isNotCompatibleWithOtherVersionSystem() {
		var self = new SemanticModVersion(0, 0, 0);
		var other = new ModVersion() {
			@Override public boolean isCompatibleWith(ModVersion other) { return false; }
			@Override public boolean isNewerThan(ModVersion other) { return false; }
			@Override public boolean matches(ModVersion other) { return false; }
		};

		assertFalse(self.isCompatibleWith(other));
	}

	@Test
	void isNotCompatibleWithPreRelease() {
		var self = new SemanticModVersion(0, 0, 1);
		var other = new SemanticModVersion(0, 0, 0);

		assertFalse(self.isCompatibleWith((ModVersion) other));
	}

	@Test
	void isNotCompatibleWithMinorOmission() {
		var self = new SemanticModVersion(1, 0, 0);
		var other = new SemanticModVersion(1, 1, 5);

		assertFalse(self.isCompatibleWith((ModVersion) other));
	}

	@Test
	void isCompatibleWithMinorAddition() {
		var self = new SemanticModVersion(1, 1, 0);
		var other = new SemanticModVersion(1, 0, 5);

		assertTrue(self.isCompatibleWith((ModVersion) other));
	}

	@Test
	void isNotCompatibleWithMajorChange() {
		var self = new SemanticModVersion(1, 1, 0);
		var other = new SemanticModVersion(2, 0, 5);

		assertFalse(self.isCompatibleWith((ModVersion) other));
	}

	@Test
	void isNotCompatibleWithPreReleaseTag() {
		var self = new SemanticModVersion(1, 0, 0, "alpha");
		var other = new SemanticModVersion(1, 0, 0);

		assertFalse(self.isCompatibleWith((ModVersion) other) || other.isCompatibleWith((ModVersion) self));
	}

	@Test
	void isNewerThan() {
		var versionStrings = List.of(
				"1.0.0-5",
				"1.0.0-alpha",
				"1.0.0-alpha.1",
				"1.0.0-alpha.beta",
				"1.0.0",
				"1.0.1",
				"1.0.11-alpha",
				"1.1.5",
				"1.11.0",
				"2.0.0"
		);

		var versions = versionStrings.stream()
				.map(SemanticModVersion::createVersion)
				.collect(Collectors.toList());

		for (int i = 0; i < versions.size(); i++) {
			assertFalse(versions.get(i).isNewerThan((ModVersion) versions.get(i)));

			for (int j = i + 1; j < versions.size(); j++) {
				assertTrue(versions.get(j).isNewerThan((ModVersion) versions.get(i)) && !versions.get(i).isNewerThan((ModVersion) versions.get(j)),
						String.format("%s < %s", versions.get(i), versions.get(j)));
			}
		}
	}

	@Test
	void matchesWithDifferingBuild() {
		var self = new SemanticModVersion(1, 0, 0, "alpha", "z45");
		var other = new SemanticModVersion(1, 0, 0, "alpha", "z100");

		assertTrue(self.matches(other));
	}
}