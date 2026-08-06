from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str) -> None:
    file = ROOT / path
    content = file.read_text(encoding="utf-8")
    if new in content:
        return
    if old not in content:
        raise RuntimeError(f"Block not found in {path}: {old[:100]!r}")
    file.write_text(content.replace(old, new, 1), encoding="utf-8")


patch(
    "app/src/main/java/com/polleg/gallery/MainActivity.kt",
    "                    onShareMedia = mediaShareLauncher::share,",
    "                    onShareMedia = { mediaShareLauncher.share(it) },",
)

patch(
    "app/src/main/java/com/polleg/gallery/ViewerActivity.kt",
    """                    is MediaMutationOutcome.Deleted,
                    is MediaMutationOutcome.Moved,
                    -> {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
""",
    """                    is MediaMutationOutcome.Deleted -> {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }

                    is MediaMutationOutcome.Moved -> {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
""",
)

patch(
    "app/src/main/java/com/polleg/gallery/ViewerActivity.kt",
    """                    folderLoadFailed -> {
                        moveTarget = null
                        LaunchedEffect(Unit) {
                            Toast.makeText(
                                (this@ViewerScreen as? Context),
                                R.string.mutation_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }

                    else -> Dialog(onDismissRequest = { moveTarget = null }) {
""",
    """                    folderLoadFailed -> Dialog(onDismissRequest = { moveTarget = null }) {
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

                    else -> Dialog(onDismissRequest = { moveTarget = null }) {
""",
)

print("Generated source fixes applied.")
