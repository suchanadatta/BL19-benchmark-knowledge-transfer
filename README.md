# 19th Century British Fiction-Nonfiction : Knowledge Transfer & Cultural Analytics for Good

This study proposes a framework for cross-genre knowledge transfer in historical information retrieval, using the British Library 19th-century digital collection as a benchmark to examine links between fiction and non-fiction retrieval. 

## Collection
- We analyze `two distinct subsets` of the BL19 collection, classified according to the metadata provided by the British Library.
- The first subset consists of `10,210` English language works of fiction, published between `1830` and `1899`.
- The second subset comprises `15,780` English language non-fiction books from the same period.

- **Metadata** : Download [metadata](https://drive.google.com/drive/folders/1yL2xmrf-UuEtNsHsMsRXxtUQ53mOjfR4?usp=drive_link) here.
   
- **Lucene Index** : The [Lucene (version 5)](https://drive.google.com/drive/folders/1_H8Uc5qwqNkpcnrJizsRBRE2Taz2w0UY?usp=drive_link) index of the metadata can be accessed here.
  
## Topics
- The selection of topics followed a structured, expert-informed process to ensure domain relevance and diversity.
- Experts proposed candidate topics based on their knowledge of genres, historical issues, and key debates.
- Through iterative consultation, `35` potential [queries](query.tsv) were finalized, representing a balanced mix of fictional and non-fictional contexts. 
- Considering common searching patterns in both scholarly and public search behavior, we kept the queries intentionally with an average query length of `2.8`.

## LLM-assisted Relevance Judgements
- For each of the `35` expert-curated queries, we retrieved the top `100` documents using the BM25.
- We used `gpt-5-mini` to judge those documents.
- Judgments were made on a graded relevance scale from '0' to '4'.
  - `0` → Not Relevant,
  - `1` → Marginally Relevant,
  - `2` → Fairly Relevant,
  - `3` → Highly Relevant, and
  - `4` → Perfectly Relevant
- We provided LLM with the `topic description` and the `document metadata`, instructing it to assess their relevance.
- We used the following [prompt](codes/create_qrel.py) while assessing the documents by the LLM.
<pre>
	"You are a helpful assistant doing graded relevance assessment. Tell me whether the following snippet is relevant 
   to the query or not. On a scale of 0 to 4, score the document where 0 indicates non-relevant and 4 being the highly 
   relevant."
</pre>
- Each judgment has 4 attributes - `topic_id`, `unused Q0`, `paragraph_id`, `relevance_score`.
- We finally obtain a pool of `3500` LLM-annotated [fiction relevance judgments](fiction.qrels) and `3500` [non-fiction relevance judgments](nonfiction.qrels).
- A randomly chosen `10% sample` of the annotations were validated by field experts which confirmed `100%` agreement.

## Cross-genre Knowledge Transfer 
- We base our knowledge transfer framework on a [RLM-based query expansion](BL-RLM/) setup.
- We apply following two cross-genre evaluation models.
  - `Fiction(RLM)` RLM is trained on the fiction collection to learn narrative and semantic expansion terms, which are then applied to retrieve non-fiction documents.
  - `Fiction-Nonfiction(RLM)` Combines the fiction and non-fiction subsets into a unified collection. RLM feedback is computed over this joint corpus, allowing the relevance model to incorporate term distributions and thematic signals from both genres.
    

## Evaluation


