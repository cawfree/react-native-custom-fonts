import React, {useContext, useEffect, useState, useRef} from "react";
import PropTypes from "prop-types";
import {typeCheck} from "type-check";
import {Platform, findNodeHandle, NativeModules} from "react-native";

const {RNCustomFonts} = NativeModules;

const throwOnInvalidUri = (uri) => {
  if (!typeCheck("String", uri) || uri.length <= 0) {
    throw new Error(`Expected String uri, encountered ${uri}.`);
  }
  return `${uri}`;
};

const throwOnInvalidFontFamily = (fontFamily) => {
  if (!typeCheck("String", fontFamily) || fontFamily.length <= 0) {
    throw new Error(`Expected String fontFamily, encountered ${fontFamily}.`);
  }
  return `${fontFamily}`;
};

const throwOnInvalidFontWeight = (fontWeight) => {
  if (typeCheck("String", fontWeight) && fontWeight.length >= 0) {
    return `${fontWeight}`;
  } else if (fontWeight === undefined) {
    return 'Normal';
  }
  throw new Error(`Expected non-empty String or undefined fontWeight, encountered ${fontWeight}.`);
}

const modulateFontWeight = fontWeight => (Platform.OS === 'ios') ? fontWeight : fontWeight.toLowerCase();

const defaultFontFaces = Object.freeze({});

const defaultFallback = Object.freeze({
  color: '#000000',
  fontFamily: Platform.OS === 'ios' ? 'Avenir' : 'Roboto',
  fontWeight: modulateFontWeight('normal'),
});

const defaultContext = Object.freeze({
  fontFaces: defaultFontFaces,
  fallback: defaultFallback,
});

const CustomFontsContext = React.createContext(defaultContext);

const sanitizeFontFaces = (fontFaces = {}) => Object
  .fromEntries(
    Object.entries(fontFaces)
      .map(
        ([k, {uri, fontFamily, fontWeight, ...extras}]) => [
          k,
          {
            uri: throwOnInvalidUri(uri),
            fontFamily: throwOnInvalidFontFamily(fontFamily),
            fontWeight: modulateFontWeight(throwOnInvalidFontWeight(fontWeight)),
            ...extras,
          },
        ],
      ),
  );

const CustomFontsProvider = ({ children, fontFaces, fallback, onDownloadDidStart, onDownloadDidEnd, onDownloadDidError, ...extraProps }) => {
  const [state, setState] = useState(defaultContext);
  useEffect(
    () => {
      const nextState = Object.freeze({
        fontFaces: sanitizeFontFaces(fontFaces),
        fallback: Object.freeze(fallback),
      });
      onDownloadDidStart();
      return RNCustomFonts
        .onFontFacesChanged(Object.values(nextState.fontFaces))
        .then(() => setState(nextState))
        .then(onDownloadDidEnd)
        .catch(
          (e) => {
            if (__DEV__) {
              console.warn(`Failed to load fonts.`);
            }
            setState(defaultContext);
            return onDownloadDidError(e);
          },
        ) && undefined;
    },
    [fontFaces, onDownloadDidStart, onDownloadDidEnd, onDownloadDidError, fallback, setState],
  );
  return (
    <CustomFontsContext.Provider
      value={state}
      children={children}
    />
  );
};

const getSafeCustomStyle = ({uri, fontFamily, fontWeight, ...extras}) => {
  if (Platform.OS === 'ios') {
    return {
      fontFamily,
      fontWeight,
      ...extras,
    };
  }
  // XXX: This is a hack; Android does not play nicely with conflicting font assignments.
  //      These override our direct calls to setTypeface().
  return extras;
};

export const useCustomFont = (name, ref = undefined) => {
  const context = useContext(CustomFontsContext);
  const [style, setStyle] = useState(fallback);

  // XXX: Evaluate fonts.
  const {fontFaces, fallback} = context;
  const {[name]: fontFace} = fontFaces;
  const hasCustomFontFace = typeCheck("Object", fontFace);

  // XXX: Evaluate refs.
  const localRef = useRef();
  const resolvedRef = ref || localRef;

  useEffect(
    () => {
      if (hasCustomFontFace) {
        const {fontFamily, fontWeight} = fontFace;
        return RNCustomFonts
          .onRequestFontFamily(
            findNodeHandle(resolvedRef.current),
            fontFamily,
            fontWeight,
          )
          .then(() => setStyle(getSafeCustomStyle(fontFace)))
          .catch(
            (e) => {
              console.error(e);
              setStyle(fallback);
            },
          )&& undefined;
      }
      setStyle(fallback);
      return undefined;
    },
    [resolvedRef, fallback, name, fontFaces, fontFace, hasCustomFontFace, setStyle],
  );

  return {
    style,
    ref: resolvedRef,
  };
};

CustomFontsProvider.propTypes = {
  fontFaces: PropTypes.shape({}),
  fallback: PropTypes.shape({}),
  onDownloadDidStart: PropTypes.func,
  onDownloadDidEnd: PropTypes.func,
  onDownloadDidError: PropTypes.func,
};

CustomFontsProvider.defaultProps = {
  fontFaces: defaultFontFaces,
  fallback: defaultFallback,
  onDownloadDidStart: () => null,
  onDownloadDidEnd: () => null,
  onDownloadDidError: () => null,
};

export default CustomFontsProvider;
