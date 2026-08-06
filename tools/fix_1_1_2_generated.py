from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def replace_text(path: str, old: str, new: str) -> None:
    file = ROOT / path
    content = file.read_text(encoding="utf-8")
    if new in content:
        return
    if old not in content:
        raise RuntimeError(f"Block not found in {path}: {old[:100]!r}")
    file.write_text(content.replace(old, new, 1), encoding="utf-8")


def replace_regex(path: str, pattern: str, replacement: str) -> None:
    file = ROOT / path
    content = file.read_text(encoding="utf-8")
    updated, count = re.subn(pattern, replacement, content, count=1, flags=re.DOTALL)
    if count == 0:
        if replacement.strip() in content:
            return
        raise RuntimeError(f"Pattern not found in {path}: {pattern[:100]!r}")
    file.write_text(updated, encoding="utf-8")


replace_text(
    "app/src/main/java/com/polleg/gallery/MainActivity.kt",
    "onShareMedia = mediaShareLauncher::share,",
    "onShareMedia = { mediaShareLauncher.share(it) },",
)

replace_regex(
    "app/src/main/java/com/polleg/gallery/ViewerActivity.kt",
    r"is MediaMutationOutcome\.Deleted,\s*is MediaMutationOutcome\.Moved,\s*->\s*\{\s*setResult\(Activity\.RESULT_OK\)\s*finish\(\)\s*\}",
    """is MediaMutationOutcome.Deleted -> {
                setResult(Activity.RESULT_OK)
                finish()
            }

            is MediaMutationOutcome.Moved -> {
                setResult(Activity.RESULT_OK)
                finish()
            }""",
)

replace_regex(
    "app/src/main/java/com/polleg/gallery/ViewerActivity.kt",
    r"folderLoadFailed\s*->\s*\{\s*moveTarget\s*=\s*null\s*LaunchedEffect\(Unit\)\s*\{.*?\}\s*\}\s*else\s*->\s*Dialog\(onDismissRequest\s*=\s*\{\s*moveTarget\s*=\s*null\s*\}\)\s*\{",
    """folderLoadFailed -> Dialog(onDismissRequest = { moveTarget = null }) {
                Surface(shape = MaterialTheme.shapes.large) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.mutation_failed))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { moveTarget = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }

            else -> Dialog(onDismissRequest = { moveTarget = null }) {""",
)

print("Generated source fixes applied.")
