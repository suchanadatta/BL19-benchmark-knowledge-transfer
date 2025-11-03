import openai, csv, argparse
from openai import OpenAI

client = OpenAI(api_key=api_key)


def assess_relevance(query, description, paragraph, model="gpt-5"):

    system_content = f"""You are a helpful assistant doing binary relevance assessment. Decide whether the given 
        paragraph is relevant to the keyword query and its description. Tell me whether the following ' \
          'snippet is relevant to the query or not. On a scale of 0 to 4, score the document where 0 indicates'\
        non-relevant and 4 being the highly relevant'"""

    user_content = f"""Keyword query: {query} Query description: {description} Paragraph: {paragraph} Answer:"""

    response = client.chat.completions.create(
        model = model,
        messages=[
            {"role": "system", "content": system_content},
            {"role": "user", "content": user_content}
        ],
    )
    result = response.choices[0].message.content
    # if result == 'relevant': return '1'
    # else: return '0'
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--query_file', default='./test_data/bl_query.tsv')
    parser.add_argument('--document_file', default='./test_data/bl_res_file.tsv')
    parser.add_argument('--qrel_file', default='./test_data/bl_qrels.tsv')
    args = parser.parse_args()

    query = dict()
    document = dict()

    # reading the query file in a dictionary
    with open(args.query_file) as csvfile:
        rows = csv.reader(csvfile, delimiter='\t')
        for row in rows:
            query[row[0]] = tuple((row[1], row[2]))
    print('query dictionary:', query)

    # reading the documents to judge in a dictionary
    last_read_qid = ''
    doclist = []
    with open(args.document_file) as csvfile:
        rows = csv.reader(csvfile, delimiter='\t')
        for row in rows:
            if last_read_qid == row[0]:
                doclist.append((row[1], row[2]))
            else:
                if len(doclist) != 0:
                    document[last_read_qid] = doclist
                    doclist = []
                last_read_qid = row[0]
                doclist.append((row[1], row[2]))
    document[row[0]] = doclist

    qrel_file = open(args.qrel_file, 'w')
    writeRelJudge = ''
    for key, value in query.items():
        qtext, qnarr = value[0], value[1]
        print('\n\n======== start annotation for query : ', key, '========')
        print('query text : ', qtext, ':::: query narration : ', qnarr)
        if key in document.keys():
            docListAssess = document[key]
            # print(docListAssess)
            print('Total documents to be assessed : ', len(docListAssess), '\n')
            for doc in docListAssess:
                print('\nassessing : ', doc[1])
                rel_score = assess_relevance(qtext, qnarr, doc[1])
                writeRelJudge += key + '\tQ0\t' + doc[0] + '\t' + doc[1] + '\t' + rel_score + '\n'
        qrel_file.write(writeRelJudge)
        writeRelJudge = ''


if __name__ == "__main__":
    main()

