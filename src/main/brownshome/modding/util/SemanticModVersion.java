package brownshome.modding.util;

import brownshome.modding.ModVersion;

import java.math.BigInteger;
import java.util.Objects;

public final class SemanticModVersion extends ModVersion {
	private final int major, minor, patch;
	private final String preReleaseCode;
	private final String buildMetadata;

	public static SemanticModVersion createVersion(String versionString) {
		// Semantic version strings can only contain [0-9A-Za-z-.+]

		class Parser {
			int minor, major, patch;
			String preReleaseCode, buildMetadata;

			CharSequence sourceData;

			Parser(CharSequence sourceData) { this.sourceData = sourceData; }

			SemanticModVersion parse() {
				major = extractNumber();
				skipDot();
				minor = extractNumber();
				skipDot();
				patch = extractNumber();

				populateReleaseAndBuild();

				return new SemanticModVersion(major, minor, patch, preReleaseCode, buildMetadata);
			}

			private void skipDot() {
				sourceData = sourceData.subSequence(1, sourceData.length());
			}

			private int extractNumber() {
				int n = 0;
				int i;

				for (i = 0; i < sourceData.length(); i++) {
					char c = sourceData.charAt(i);

					if (c == '.' || c == '-' || c == '+') break;

					if (c < '0' || c > '9') throw new NumberFormatException();

					n = n * 10 + c - '0';
				}

				sourceData = sourceData.subSequence(i, sourceData.length());

				return n;
			}

			private void populateReleaseAndBuild() {
				if (sourceData.length() == 0) {
					return;
				}

				char c = sourceData.charAt(0);

				boolean hasReleaseCode = c == '-';

				for (int i = 1; i < sourceData.length(); i++) {
					c = sourceData.charAt(i);

					if (sourceData.charAt(i) == '+') {
						assert hasReleaseCode;

						preReleaseCode = sourceData.subSequence(1, i).toString();
						buildMetadata = sourceData.subSequence(i + 1, sourceData.length()).toString();

						continue;
					}

					if((c < '0' || c > '9') && (c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && c != '-' && c != '.') {
						throw new IllegalArgumentException("'" + c + "' is not a valid character in semantic versioning");
					}
				}

				if (hasReleaseCode) {
					if (preReleaseCode == null) {
						preReleaseCode = sourceData.subSequence(1, sourceData.length()).toString();
					}
				} else {
					assert buildMetadata == null;

					buildMetadata = sourceData.subSequence(1, sourceData.length()).toString();
				}
			}
		}

		return new Parser(versionString).parse();
	}

	public SemanticModVersion(int major, int minor, int patch, String preReleaseCode, String buildMetadata) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.preReleaseCode = preReleaseCode;
		this.buildMetadata = buildMetadata;
	}

	public SemanticModVersion(int major, int minor, int patch, String preReleaseCode) {
		this(major, minor, patch, preReleaseCode, null);
	}

	public SemanticModVersion(int major, int minor, int patch) {
		this(major, minor, patch, null);
	}

	public int major() {
		return major;
	}

	public int minor() {
		return minor;
	}

	public int patch() {
		return patch;
	}

	public String preReleaseCode() {
		return preReleaseCode;
	}

	public String buildMetadata() {
		return buildMetadata;
	}

	public boolean isCompatibleWith(SemanticModVersion other) {
		if (other.major == 0 || major == 0 || preReleaseCode != null || other.preReleaseCode != null) {
			// Everything except build numbers must match for pre-release versions

			return matches(other);
		}

		if (other.major != major)
			return false;

		return minor >= other.minor;
	}

	@Override
	public boolean isCompatibleWith(ModVersion other) {
		if (!(other instanceof SemanticModVersion)) {
			return false;
		}

		return isCompatibleWith((SemanticModVersion) other);
	}

	public boolean isNewerThan(SemanticModVersion other) {
		if (other.major != major)
			return major > other.major;

		if (other.minor != minor)
			return minor > other.minor;

		if (other.patch != patch)
			return patch > other.patch;

		if (preReleaseCode == null)
			return other.preReleaseCode != null;

		// We have a pre-release code

		if (other.preReleaseCode == null) {
			return false;
		}

		// Compare the codes

		String[] splitCode = preReleaseCode.split("\\.");
		String[] otherSplitCode = other.preReleaseCode.split("\\.");

		for (int i = 0; i < splitCode.length && i < otherSplitCode.length; i++) {
			String code = splitCode[i], otherCode = otherSplitCode[i];
			int compareValue;

			BigInteger intParse;

			try {
				intParse = new BigInteger(code);
			} catch(NumberFormatException nfe) {
				intParse = null;
			}

			BigInteger otherIntParse;

			try {
				otherIntParse = new BigInteger(otherCode);
			} catch(NumberFormatException nfe) {
				otherIntParse = null;
			}

			if (intParse == null && otherIntParse == null) {
				compareValue = code.compareTo(otherCode);
			} else if (intParse != null && otherIntParse != null) {
				compareValue = intParse.compareTo(otherIntParse);
			} else {
				// Numeric codes are never newer than non-numeric codes
				return otherIntParse != null;
			}

			if (compareValue != 0) {
				return compareValue > 0;
			}
		}

		// The longer code is newer
		return otherSplitCode.length < splitCode.length;
	}

	@Override
	public boolean isNewerThan(ModVersion other) {
		if (!(other instanceof SemanticModVersion)) {
			return false;
		}

		return isNewerThan((SemanticModVersion) other);
	}

	public boolean matches(SemanticModVersion other) {
		return major == other.major &&
				minor == other.minor &&
				patch == other.patch &&
				Objects.equals(preReleaseCode, other.preReleaseCode);
	}

	@Override
	public boolean matches(ModVersion other) {
		if (!(other instanceof SemanticModVersion)) {
			return false;
		}

		return matches((SemanticModVersion) other);
	}

	@Override
	public boolean equals(Object o) {
		if(this == o)
			return true;

		if(!(o instanceof SemanticModVersion))
			return false;

		SemanticModVersion that = (SemanticModVersion) o;

		return major == that.major &&
				minor == that.minor &&
				patch == that.patch &&
				Objects.equals(preReleaseCode, that.preReleaseCode) &&
				Objects.equals(buildMetadata, that.buildMetadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch, preReleaseCode, buildMetadata);
	}

	@Override
	public String toString() {
		StringBuilder suffix = new StringBuilder();

		if (preReleaseCode != null) {
			suffix.append("-").append(preReleaseCode);
		}

		if (buildMetadata != null) {
			suffix.append("+").append(buildMetadata);
		}

		return String.format("%d.%d.%d%s", major, minor, patch, suffix);
	}
}
