import { getDependencies } from '../API/api'; // Adjust the path if needed

const SendBack = ({ editorRef, language }) => {
  const runCode = async () => {
    const sourceCode = editorRef.current.getValue();
    if (!sourceCode) return;
    try {
      const resp = await getDependencies(language, sourceCode);
      if (Array.isArray(resp)) {
        resp.forEach((msg, idx) => console.log(`Result ${idx + 1}:\n` + msg));
      } else {
        console.log(resp);
      }
    } catch (error) {
      // Error handling
    }
  };

  return (
    <div>
      <button onClick={runCode}>
        Send Code
      </button>
    </div>
  );
};

export default SendBack;