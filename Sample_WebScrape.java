    private static class GetVerseCommentaryTask extends AsyncTask<String, Void, List<SpannableString>> {

        private final WeakReference<ResearchVerses_BibleRef> dialogReference;
        private final int colorPrimary;

        // Only retain a weak reference to the activity. Allows access to parent class view
        // while keeping innerclass static to prevent memory leaks. Source: https://stackoverflow.com/a/46166223/848353
        GetVerseCommentaryTask(ResearchVerses_BibleRef context, int colorPrimary) {
            dialogReference = new WeakReference<>(context);
            this.colorPrimary = colorPrimary;
        }

        @Override
        protected List<SpannableString> doInBackground(String... params) {
            String urlBibleRef = params[0];
            List<SpannableString> taskResults = new ArrayList<>();
            StringBuilder verseContext = new StringBuilder();
            String websiteTitle = "";

            Document doc;
            try {
                doc = Jsoup.connect(urlBibleRef).get();

                websiteTitle = doc.title();

                // Verse commentary is stored in the leftcomment class
                List<Node> nodes = doc.select("div.leftcomment").first().childNodes();
                for (Node node : nodes) {

                    // Don't include the BibleRef 'What does [verse] mean
                    if (!node.outerHtml().contains("<h1>What does ")) {
                        // API 24 check Source: https://stackoverflow.com/a/41585220/848353
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            verseContext.append(Html.fromHtml(node.outerHtml(), Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            verseContext.append(Html.fromHtml(node.outerHtml()));
                        }
                    }
                }

                String urlSection = doc.select("div.righttext").select("script").get(0).attributes().get("src");
                String urlChapter = doc.select("div.belowtext").select("script").get(0).attributes().get("src");

                //  Source: https://stackoverflow.com/a/41954213/848353
                SpannableStringBuilder copyrightAuthor = new SpannableStringBuilder();
                copyrightAuthor.append("BibleRef.com");
                copyrightAuthor.setSpan(new URLSpan("https://www.bibleref.com/about.html"), 0, copyrightAuthor.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                copyrightAuthor.setSpan(new ForegroundColorSpan(colorPrimary), 0, copyrightAuthor.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                SpannableStringBuilder copyrightTitle = new SpannableStringBuilder();
                copyrightTitle.append(websiteTitle);
                copyrightTitle.setSpan(new URLSpan(urlBibleRef), 0, copyrightTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                copyrightTitle.setSpan(new ForegroundColorSpan(colorPrimary), 0, copyrightTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                taskResults.add(SpannableString.valueOf(verseContext.toString()));
                taskResults.add(SpannableString.valueOf(urlSection));
                taskResults.add(SpannableString.valueOf(urlChapter));
                taskResults.add(SpannableString.valueOf(copyrightAuthor));
                taskResults.add(SpannableString.valueOf(copyrightTitle));

            } catch (IOException e) {
                e.printStackTrace();
            }

            return taskResults;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            dialogReference.get().pbVerse.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(List<SpannableString> taskResult) {

            dialogReference.get().pbVerse.setVisibility(View.INVISIBLE);

            // Set verse context
            dialogReference.get().tvVerseContext.setText(taskResult.get(0));

            // Verse Default
            dialogReference.get().tvVerseContext.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                // Source: https://stackoverflow.com/a/45252000/848353
                @Override
                public boolean onPreDraw() {
                    // Remove listener because we don't want this called before every frame
                    dialogReference.get().tvVerseContext.getViewTreeObserver().removeOnPreDrawListener(this);

                    // Drawing happens after layout so we can assume getLineCount() returns the correct value
                    if (dialogReference.get().tvVerseContext.getLineCount() > 3) {
                        dialogReference.get().btVerseContext.setText(R.string.commentary_readMore);
                        dialogReference.get().btVerseContext.setVisibility(View.VISIBLE);
                        dialogReference.get().tvVerseContext.setMaxLines(3);
                        dialogReference.get().vVerseContextGradient.setVisibility(View.VISIBLE);
                    } else {
                        dialogReference.get().btVerseContext.setVisibility(View.GONE);
                        dialogReference.get().tvVerseContext.setMaxLines(Integer.MAX_VALUE);
                        dialogReference.get().vVerseContextGradient.setVisibility(View.GONE);
                    }

                    return true; // true because we don't want to skip this frame
                }
            });

            // Set section context
            GetSectionCommentaryTask getSectionCommentaryTask = new GetSectionCommentaryTask(dialogReference);
            getSectionCommentaryTask.execute(taskResult.get(1).toString());

            // Set chapter context
            GetChapterCommentaryTask getChapterCommentaryTask = new GetChapterCommentaryTask(dialogReference);
            getChapterCommentaryTask.execute(taskResult.get(2).toString());

            // Set copyright author and then enable clicking on the url span
            dialogReference.get().tvCopyrightAuthor.setText(taskResult.get(3));
            dialogReference.get().tvCopyrightAuthor.setMovementMethod(LinkMovementMethod.getInstance());

            // Set copyright title and then enable clicking on the url span
            dialogReference.get().tvCopyrightTitle.setText(taskResult.get(4));
            dialogReference.get().tvCopyrightTitle.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }