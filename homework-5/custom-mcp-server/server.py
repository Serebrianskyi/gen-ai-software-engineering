from fastmcp import FastMCP
import os

mcp = FastMCP("Lorem Ipsum MCP")

LOREM_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "lorem-ipsum.md")


def _read_words(word_count: int) -> str:
    with open(LOREM_FILE) as f:
        words = f.read().split()
    return " ".join(words[:word_count])


# Resources are URIs that Claude can read from (e.g., files, APIs).
@mcp.resource("lorem://text/{word_count}")
def lorem_resource(word_count: int) -> str:
    """Returns exactly word_count words from lorem-ipsum.md."""
    return _read_words(word_count)


# Tools are actions Claude can call to perform operations (e.g., reading a file, running a command).
@mcp.tool()
def read(word_count: int = 30) -> str:
    """Read content from lorem-ipsum.md, returning exactly word_count words."""
    return _read_words(word_count)


if __name__ == "__main__":
    mcp.run()