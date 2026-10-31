import unittest
from unittest.mock import patch

from validate_stream_durations import (
    apply_verified_durations,
    build_audit_report,
    collect_streams,
    format_duration_seconds,
    verify_raw_items,
)


class ValidateStreamDurationsTest(unittest.TestCase):
    def test_actual_duration_format_preserves_minutes_and_seconds(self):
        self.assertEqual("2 min 18 sec", format_duration_seconds(138.2))
        self.assertEqual("2 h 14 min", format_duration_seconds(8_040))

    def test_duplicate_category_rows_are_probed_once(self):
        url = "https://cdn.example.org/feature.mp4"
        content = {
            "featured": {"id": "featured", "title": "Feature", "videoUrl": url},
            "categories": [
                {"name": "Movies", "items": [{"id": "movie", "title": "Feature", "videoUrl": url}]},
                {"name": "Drama", "items": [{"id": "drama", "title": "Feature", "videoUrl": url}]},
            ],
        }
        self.assertEqual({url}, set(collect_streams(content)))

    def test_raw_import_keeps_measured_clips_classified_and_drops_probe_failures(self):
        items = [
            {"id": "full", "title": "Full", "category": "Movies", "videoUrl": "https://cdn.example.org/full.mp4"},
            {"id": "clip", "title": "Clip", "category": "Movies", "videoUrl": "https://cdn.example.org/clip.mp4"},
            {"id": "unknown", "title": "Unknown", "category": "Movies", "videoUrl": "https://cdn.example.org/unknown.mp4"},
        ]
        probe_results = {
            "https://cdn.example.org/full.mp4": {"actualDurationSeconds": 5_400, "error": None},
            "https://cdn.example.org/clip.mp4": {"actualDurationSeconds": 138, "error": None},
            "https://cdn.example.org/unknown.mp4": {"actualDurationSeconds": None, "error": "timeout"},
        }

        with patch("validate_stream_durations.probe_streams", return_value=probe_results):
            measured, report = verify_raw_items(items)

        self.assertEqual(["full", "clip"], [item["id"] for item in measured])
        self.assertEqual(["feature", "short_clip"], [item["contentType"] for item in measured])
        self.assertEqual(1, report["summary"]["unverified"])

    def test_catalog_separates_features_from_measured_short_clips_and_drops_unverified(self):
        full_url = "https://cdn.example.org/full.mp4"
        short_url = "https://cdn.example.org/short.mp4"
        unknown_url = "https://cdn.example.org/unknown.mp4"
        content = {
            "featured": {"id": "full", "title": "Full", "duration": "2 h 14 m", "videoUrl": full_url},
            "categories": [
                {
                    "id": "movies",
                    "name": "Movies",
                    "items": [
                        {"id": "full", "title": "Full", "duration": "2 h 14 m", "videoUrl": full_url},
                        {"id": "short", "title": "Short", "duration": "2 h 14 m", "videoUrl": short_url},
                        {"id": "unknown", "title": "Unknown", "duration": "2 h 14 m", "videoUrl": unknown_url},
                    ],
                },
                {
                    "id": "drama",
                    "name": "Drama",
                    "items": [{"id": "short-copy", "title": "Short", "videoUrl": short_url}],
                },
            ],
        }
        results = {
            full_url: {"actualDurationSeconds": 8_040, "error": None},
            short_url: {"actualDurationSeconds": 138, "error": None},
            unknown_url: {"actualDurationSeconds": None, "error": "probe timed out"},
        }

        updated, summary = apply_verified_durations(content, results)

        movies = updated["categories"][0]["items"]
        clips = updated["categories"][-1]
        self.assertEqual(["full"], [item["id"] for item in movies])
        self.assertEqual(8_040, movies[0]["actualDurationSeconds"])
        self.assertEqual("2 h 14 min", movies[0]["duration"])
        self.assertEqual("Short Clips", clips["name"])
        self.assertEqual(1, len(clips["items"]))
        self.assertEqual("short", clips["items"][0]["id"])
        self.assertEqual("short_clip", clips["items"][0]["contentType"])
        self.assertEqual(138, clips["items"][0]["actualDurationSeconds"])
        self.assertEqual("2 min 18 sec", clips["items"][0]["duration"])
        self.assertEqual(
            {
                "featureRowsKept": 1,
                "shortClipsKept": 1,
                "duplicateShortRowsCollapsed": 1,
                "unverifiedRowsRemoved": 1,
            },
            summary,
        )
        self.assertEqual("full", updated["featured"]["id"])

    def test_audit_distinguishes_short_feature_and_unverified_results(self):
        streams = {
            "https://cdn.example.org/short.mp4": {"title": "Short", "duration": "2 h 14 m"},
            "https://cdn.example.org/full.mp4": {"title": "Full", "duration": "2 h 14 m"},
            "https://cdn.example.org/unknown.mp4": {"title": "Unknown", "duration": "1 h"},
        }
        report = build_audit_report(
            streams,
            {
                "https://cdn.example.org/short.mp4": {"actualDurationSeconds": 138, "error": None},
                "https://cdn.example.org/full.mp4": {"actualDurationSeconds": 8_040, "error": None},
                "https://cdn.example.org/unknown.mp4": {"actualDurationSeconds": None, "error": "timeout"},
            },
        )
        self.assertEqual(
            {
                "uniqueStreams": 3,
                "measured": 2,
                "unverified": 1,
                "featureLength": 1,
                "shorterThanFeatureLength": 1,
            },
            report["summary"],
        )


if __name__ == "__main__":
    unittest.main()
