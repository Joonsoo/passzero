with open("words.txt", "r") as f:
	words = map(lambda x: x.strip(), f.readlines())
	print len(words)
	def filterWord(word):
		return all('a' <= x and x <= 'z' for x in word) and 3 <= len(word) and len(word) <= 10
	filteredWords = filter(filterWord, words)

with open("src/main/resources/words.txt", "w") as f:
	print len(filteredWords)
	maxlength = len(max(filteredWords, key=len))
	f.write((" " * maxlength) +  "\n")
	for word in filteredWords:
		f.write((" " * (maxlength - len(word))) + word + "\n")
		first = False
